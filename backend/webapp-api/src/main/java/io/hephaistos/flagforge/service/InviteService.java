package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.InviteCreationRequest;
import io.hephaistos.flagforge.controller.dto.InviteCreationResponse;
import io.hephaistos.flagforge.controller.dto.InviteValidationResponse;
import io.hephaistos.flagforge.controller.dto.PendingInviteResponse;
import io.hephaistos.flagforge.data.CompanyInviteEntity;

import java.util.List;
import java.util.UUID;

public interface InviteService {

    InviteCreationResponse createInvite(InviteCreationRequest request, String baseUrl);

    InviteValidationResponse validateInvite(String token);

    CompanyInviteEntity getInviteByToken(String token);

    void consumeInvite(CompanyInviteEntity invite, UUID customerId);

    List<PendingInviteResponse> getPendingInvitesForCompany();
}
