package io.hephaistos.flagforge.controller.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response containing comprehensive pricing and statistics data for all applications and
 * environments within a company.
 */
public record CompanyStatisticsResponse(List<ApplicationStatistics> applications,
                                        Integer totalMonthlyPriceUsd) {

    /**
     * Statistics for a single application including all its environments.
     */
    public record ApplicationStatistics(UUID id, String name,
                                        List<EnvironmentStatistics> environments,
                                        Integer totalMonthlyPriceUsd) {
    }


    /**
     * Statistics for a single environment including tier and pricing.
     */
    public record EnvironmentStatistics(UUID id, String name, String description, String tier,
                                        Integer monthlyPriceUsd) {
    }
}
