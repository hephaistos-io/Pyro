package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.enums.CustomerRole;

import java.util.List;
import java.util.UUID;

public record UpdateCustomerRequest(List<UUID> applicationIds, CustomerRole role) {
}