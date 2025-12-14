package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.PricingTier;

import java.util.List;
import java.util.UUID;

public record ApplicationResponse(UUID id, String name, UUID companyId, PricingTier pricingTier,
                                  List<EnvironmentResponse> environments) {

    public static ApplicationResponse fromEntity(ApplicationEntity applicationEntity) {
        List<EnvironmentResponse> environments = applicationEntity.getEnvironments()
                .stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
        return new ApplicationResponse(applicationEntity.getId(), applicationEntity.getName(),
                applicationEntity.getCompanyId(), applicationEntity.getPricingTier(), environments);
    }
}
