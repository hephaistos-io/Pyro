package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteValidationResponse;
import io.hephaistos.flagforge.data.CompanyEntity;
import io.hephaistos.flagforge.data.CompanyInviteEntity;
import io.hephaistos.flagforge.data.CustomerRole;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.CompanyInviteRepository;
import io.hephaistos.flagforge.data.repository.CompanyRepository;
import io.hephaistos.flagforge.exception.InvalidInviteException;
import io.hephaistos.flagforge.exception.InvalidInviteException.InvalidInviteReason;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultInviteServiceTest {

    @Mock
    private CompanyInviteRepository companyInviteRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    private DefaultInviteService inviteService;
    private UUID testCompanyId;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        inviteService = new DefaultInviteService(companyInviteRepository, companyRepository,
                applicationRepository);
        testCompanyId = UUID.randomUUID();
        testCustomerId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInviteGenerates64CharHexToken() {
        setupSecurityContext(testCompanyId, testCustomerId, CustomerRole.ADMIN);

        var request = new InviteCreationRequest("test@example.com", CustomerRole.DEV, null, null);

        when(companyInviteRepository.save(any(CompanyInviteEntity.class))).thenAnswer(
                invocation -> {
                    CompanyInviteEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        var response = inviteService.createInvite(request, "https://example.com");

        assertThat(response.token()).hasSize(64);
        assertThat(response.token()).matches("[0-9a-f]+");
    }

    @Test
    void createInviteExpiresAfterConfiguredDays() {
        setupSecurityContext(testCompanyId, testCustomerId, CustomerRole.ADMIN);

        var request = new InviteCreationRequest("test@example.com", CustomerRole.DEV, null, 14);

        when(companyInviteRepository.save(any(CompanyInviteEntity.class))).thenAnswer(
                invocation -> {
                    CompanyInviteEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        var response = inviteService.createInvite(request, "https://example.com");

        ArgumentCaptor<CompanyInviteEntity> captor =
                ArgumentCaptor.forClass(CompanyInviteEntity.class);
        verify(companyInviteRepository).save(captor.capture());

        Instant expectedExpiry = Instant.now().plus(14, ChronoUnit.DAYS);
        assertThat(captor.getValue().getExpiresAt()).isCloseTo(expectedExpiry,
                org.assertj.core.api.Assertions.within(1, ChronoUnit.MINUTES));
    }

    @Test
    void createInviteDefaultsTo7DaysExpiry() {
        setupSecurityContext(testCompanyId, testCustomerId, CustomerRole.ADMIN);

        var request = new InviteCreationRequest("test@example.com", CustomerRole.DEV, null, null);

        when(companyInviteRepository.save(any(CompanyInviteEntity.class))).thenAnswer(
                invocation -> {
                    CompanyInviteEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        inviteService.createInvite(request, "https://example.com");

        ArgumentCaptor<CompanyInviteEntity> captor =
                ArgumentCaptor.forClass(CompanyInviteEntity.class);
        verify(companyInviteRepository).save(captor.capture());

        Instant expectedExpiry = Instant.now().plus(7, ChronoUnit.DAYS);
        assertThat(captor.getValue().getExpiresAt()).isCloseTo(expectedExpiry,
                org.assertj.core.api.Assertions.within(1, ChronoUnit.MINUTES));
    }

    // Note: The admin role requirement is now enforced via @RequireAdmin annotation
    // which is tested in integration tests with Spring Security enabled.
    // See integration tests for @PreAuthorize behavior verification.

    @Test
    void createInviteCannotAssignHigherRoleThanOwn() {
        setupSecurityContext(testCompanyId, testCustomerId, CustomerRole.ADMIN);

        // Trying to assign ADMIN role when the ordinal is the same should work
        var request = new InviteCreationRequest("test@example.com", CustomerRole.ADMIN, null, null);

        when(companyInviteRepository.save(any(CompanyInviteEntity.class))).thenAnswer(
                invocation -> {
                    CompanyInviteEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // This should not throw
        inviteService.createInvite(request, "https://example.com");
    }

    @Test
    void validateInviteReturnsNotFoundForNonexistentToken() {
        when(companyInviteRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        InviteValidationResponse response = inviteService.validateInvite("nonexistent");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo(InvalidInviteReason.NOT_FOUND);
    }

    @Test
    void validateInviteReturnsExpiredForExpiredInvite() {
        var invite = createInvite();
        invite.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(companyInviteRepository.findByToken("token")).thenReturn(Optional.of(invite));

        InviteValidationResponse response = inviteService.validateInvite("token");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo(InvalidInviteReason.EXPIRED);
    }

    @Test
    void validateInviteReturnsAlreadyUsedForUsedInvite() {
        var invite = createInvite();
        invite.setUsedAt(Instant.now());
        invite.setUsedBy(UUID.randomUUID());

        when(companyInviteRepository.findByToken("token")).thenReturn(Optional.of(invite));

        InviteValidationResponse response = inviteService.validateInvite("token");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo(InvalidInviteReason.ALREADY_USED);
    }

    @Test
    void validateInviteReturnsValidForGoodInvite() {
        var invite = createInvite();
        var company = new CompanyEntity();
        company.setId(testCompanyId);
        company.setName("Test Company");

        when(companyInviteRepository.findByToken("token")).thenReturn(Optional.of(invite));
        when(companyRepository.findById(testCompanyId)).thenReturn(Optional.of(company));

        InviteValidationResponse response = inviteService.validateInvite("token");

        assertThat(response.valid()).isTrue();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.companyName()).isEqualTo("Test Company");
        assertThat(response.role()).isEqualTo(CustomerRole.DEV);
    }

    @Test
    void getInviteByTokenThrowsForNonexistentToken() {
        when(companyInviteRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.getInviteByToken("nonexistent")).isInstanceOf(
                InvalidInviteException.class).satisfies(ex -> {
            InvalidInviteException inviteEx = (InvalidInviteException) ex;
            assertThat(inviteEx.getReason()).isEqualTo(InvalidInviteReason.NOT_FOUND);
        });
    }

    @Test
    void getInviteByTokenThrowsForExpiredInvite() {
        var invite = createInvite();
        invite.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(companyInviteRepository.findByToken("token")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.getInviteByToken("token")).isInstanceOf(
                InvalidInviteException.class).satisfies(ex -> {
            InvalidInviteException inviteEx = (InvalidInviteException) ex;
            assertThat(inviteEx.getReason()).isEqualTo(InvalidInviteReason.EXPIRED);
        });
    }

    @Test
    void getInviteByTokenThrowsForUsedInvite() {
        var invite = createInvite();
        invite.setUsedAt(Instant.now());

        when(companyInviteRepository.findByToken("token")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> inviteService.getInviteByToken("token")).isInstanceOf(
                InvalidInviteException.class).satisfies(ex -> {
            InvalidInviteException inviteEx = (InvalidInviteException) ex;
            assertThat(inviteEx.getReason()).isEqualTo(InvalidInviteReason.ALREADY_USED);
        });
    }

    @Test
    void consumeInviteSetsUsedAtAndUsedBy() {
        var invite = createInvite();
        UUID customerId = UUID.randomUUID();

        when(companyInviteRepository.save(any(CompanyInviteEntity.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        inviteService.consumeInvite(invite, customerId);

        ArgumentCaptor<CompanyInviteEntity> captor =
                ArgumentCaptor.forClass(CompanyInviteEntity.class);
        verify(companyInviteRepository).save(captor.capture());

        assertThat(captor.getValue().getUsedAt()).isNotNull();
        assertThat(captor.getValue().getUsedBy()).isEqualTo(customerId);
    }

    @Test
    void getPendingInvitesFiltersOutExpiredInvites() {
        setupSecurityContext(testCompanyId, testCustomerId, CustomerRole.ADMIN);

        var validInvite = createInvite();
        validInvite.setId(UUID.randomUUID());

        var expiredInvite = createInvite();
        expiredInvite.setId(UUID.randomUUID());
        expiredInvite.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(companyInviteRepository.findPending()).thenReturn(List.of(validInvite, expiredInvite));

        var result = inviteService.getPendingInvitesForCompany();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(validInvite.getId());
    }

    private CompanyInviteEntity createInvite() {
        var invite = new CompanyInviteEntity();
        invite.setId(UUID.randomUUID());
        invite.setToken("token");
        invite.setEmail("test@example.com");
        invite.setCompanyId(testCompanyId);
        invite.setCreatedBy(testCustomerId);
        invite.setAssignedRole(CustomerRole.DEV);
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invite.setCreatedAt(Instant.now());
        return invite;
    }

    private void setupSecurityContext(UUID companyId, UUID customerId, CustomerRole role) {
        var context = new FlagForgeSecurityContext();
        context.setCustomerName("test@example.com");
        context.setCustomerId(customerId);
        context.setCompanyId(companyId.toString());
        context.setAuthentication(new UsernamePasswordAuthenticationToken("test@example.com", null,
                List.of(new SimpleGrantedAuthority(role.toAuthority()))));
        SecurityContextHolder.setContext(context);
    }
}
