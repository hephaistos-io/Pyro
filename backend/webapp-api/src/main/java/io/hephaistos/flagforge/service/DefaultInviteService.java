package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyInviteEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.controller.dto.InviteValidationResponse;
import io.hephaistos.flagforge.controller.dto.PendingInviteResponse;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyInviteRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.exception.InvalidInviteException;
import io.hephaistos.flagforge.exception.InvalidInviteException.InvalidInviteReason;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.exception.OperationNotAllowedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import io.hephaistos.flagforge.security.RequireAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class DefaultInviteService implements InviteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInviteService.class);
    private static final int INVITE_TOKEN_LENGTH = 32;
    private static final int DEFAULT_EXPIRATION_DAYS = 7;

    private final CompanyInviteRepository companyInviteRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public DefaultInviteService(CompanyInviteRepository companyInviteRepository,
            CompanyRepository companyRepository, ApplicationRepository applicationRepository) {
        this.companyInviteRepository = companyInviteRepository;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    @RequireAdmin
    public InviteCreationResponse createInvite(InviteCreationRequest request, String baseUrl) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        UUID companyId = securityContext.getCompanyId()
                .orElseThrow(() -> new OperationNotAllowedException(
                        "Cannot create invite without a company"));

        // Validate that inviter is not assigning a role higher than their own
        CustomerRole inviterRole = securityContext.getRole();
        if (request.role().ordinal() > inviterRole.ordinal()) {
            throw new OperationNotAllowedException("Cannot assign a role higher than your own");
        }

        // Validate and fetch applications if provided
        Set<ApplicationEntity> applications = new HashSet<>();
        if (request.applicationIds() != null && !request.applicationIds().isEmpty()) {
            for (UUID appId : request.applicationIds()) {
                ApplicationEntity app = applicationRepository.findById(appId)
                        .orElseThrow(
                                () -> new NotFoundException("Application not found: " + appId));
                // Verify application belongs to the company
                if (!app.getCompanyId().equals(companyId)) {
                    throw new OperationNotAllowedException(
                            "Application does not belong to your company: " + appId);
                }
                applications.add(app);
            }
        }

        var invite = new CompanyInviteEntity();
        invite.setToken(generateInviteToken());
        invite.setEmail(request.email().toLowerCase());
        invite.setCompanyId(companyId);
        invite.setCreatedBy(securityContext.getCustomerId());
        invite.setAssignedRole(request.role());
        invite.setExpiresAt(Instant.now().plus(request.getExpiresInDays(), ChronoUnit.DAYS));
        invite.setCreatedAt(Instant.now());
        invite.setPreAssignedApplications(applications);

        companyInviteRepository.save(invite);
        LOGGER.info("Created invite for email {} with role {} in company {}", request.email(),
                request.role(), companyId);

        return InviteCreationResponse.fromEntity(invite, baseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public InviteValidationResponse validateInvite(String token) {
        var inviteOpt = companyInviteRepository.findByToken(token);

        if (inviteOpt.isEmpty()) {
            return InviteValidationResponse.invalid(InvalidInviteReason.NOT_FOUND);
        }

        var invite = inviteOpt.get();

        if (invite.isUsed()) {
            return InviteValidationResponse.invalid(InvalidInviteReason.ALREADY_USED);
        }

        if (invite.isExpired()) {
            return InviteValidationResponse.invalid(InvalidInviteReason.EXPIRED);
        }

        var company = companyRepository.findById(invite.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found"));

        return InviteValidationResponse.valid(invite, company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyInviteEntity getInviteByToken(String token) {
        var invite = companyInviteRepository.findByToken(token)
                .orElseThrow(() -> new InvalidInviteException("Invite not found",
                        InvalidInviteReason.NOT_FOUND));

        if (invite.isUsed()) {
            throw new InvalidInviteException("Invite has already been used",
                    InvalidInviteReason.ALREADY_USED);
        }

        if (invite.isExpired()) {
            throw new InvalidInviteException("Invite has expired", InvalidInviteReason.EXPIRED);
        }

        return invite;
    }

    @Override
    public void consumeInvite(CompanyInviteEntity invite, UUID customerId) {
        invite.setUsedAt(Instant.now());
        invite.setUsedBy(customerId);
        companyInviteRepository.save(invite);
        LOGGER.info("Invite {} consumed by customer {}", invite.getId(), customerId);
    }

    @Override
    @RequireAdmin
    @Transactional(readOnly = true)
    public List<PendingInviteResponse> getPendingInvitesForCompany() {
        return companyInviteRepository.findPending()
                .stream()
                .filter(invite -> !invite.isExpired())
                .map(PendingInviteResponse::fromEntity)
                .toList();
    }

    @Override
    @RequireAdmin
    public InviteCreationResponse regenerateInvite(UUID inviteId, String baseUrl) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        UUID companyId = securityContext.getCompanyId()
                .orElseThrow(() -> new OperationNotAllowedException(
                        "Cannot regenerate invite without a company"));

        var invite = companyInviteRepository.findByIdWithApplications(inviteId)
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        // Verify invite belongs to the user's company
        if (!invite.getCompanyId().equals(companyId)) {
            throw new NotFoundException("Invite not found");
        }

        // Cannot regenerate a used invite
        if (invite.isUsed()) {
            throw new OperationNotAllowedException(
                    "Cannot regenerate an invite that has already been used");
        }

        // Generate new token and reset expiration
        invite.setToken(generateInviteToken());
        invite.setExpiresAt(Instant.now().plus(DEFAULT_EXPIRATION_DAYS, ChronoUnit.DAYS));

        companyInviteRepository.save(invite);
        LOGGER.info("Regenerated invite {} for email {}", inviteId, invite.getEmail());

        return InviteCreationResponse.fromEntity(invite, baseUrl);
    }

    @Override
    @RequireAdmin
    public void deleteInvite(UUID inviteId) {
        var securityContext = FlagForgeSecurityContext.getCurrent();
        UUID companyId = securityContext.getCompanyId()
                .orElseThrow(() -> new OperationNotAllowedException(
                        "Cannot delete invite without a company"));

        var invite = companyInviteRepository.findById(inviteId)
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        // Verify invite belongs to the user's company
        if (!invite.getCompanyId().equals(companyId)) {
            throw new NotFoundException("Invite not found");
        }

        companyInviteRepository.delete(invite);
        LOGGER.info("Deleted invite {} for email {}", inviteId, invite.getEmail());
    }

    private String generateInviteToken() {
        byte[] randomBytes = new byte[INVITE_TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}
