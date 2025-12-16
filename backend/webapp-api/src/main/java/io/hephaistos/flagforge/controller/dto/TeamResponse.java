package io.hephaistos.flagforge.controller.dto;

import java.util.List;

public record TeamResponse(List<CustomerResponse> members,
                           List<PendingInviteResponse> pendingInvites) {
}
