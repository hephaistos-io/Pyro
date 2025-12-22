package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.CompanyEntity;
import io.hephaistos.flagforge.common.data.CompanyInviteEntity;
import io.hephaistos.flagforge.common.enums.CustomerRole;
import io.hephaistos.flagforge.exception.InvalidInviteException.InvalidInviteReason;

import java.util.List;

public record InviteValidationResponse(boolean valid, String email, String companyName,
                                       CustomerRole role, List<String> applicationNames,
                                       InvalidInviteReason reason) {
    public static InviteValidationResponse valid(CompanyInviteEntity invite,
            CompanyEntity company) {
        List<String> appNames = invite.getPreAssignedApplications()
                .stream()
                .map(ApplicationEntity::getName)
                .toList();

        return new InviteValidationResponse(true, invite.getEmail(), company.getName(),
                invite.getAssignedRole(), appNames, null);
    }

    public static InviteValidationResponse invalid(InvalidInviteReason reason) {
        return new InviteValidationResponse(false, null, null, null, null, reason);
    }
}
