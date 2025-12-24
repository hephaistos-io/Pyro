package io.hephaistos.flagforge.controller.dto;

import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Daily usage statistics for an environment")
public record DailyUsageStatisticsResponse(
        @Schema(description = "The date of the statistics", example = "2025-12-24") LocalDate date,

        @Schema(description = "Total number of requests for this day",
                example = "15432") long totalRequests,

        @Schema(description = "Peak requests per second reached during this day",
                example = "45") int peakRequestsPerSecond,

        @Schema(description = "Average requests per second for this day",
                example = "0.18") BigDecimal avgRequestsPerSecond,

        @Schema(description = "Number of requests rejected due to rate limiting",
                example = "42") long rejectedRequests) {
    public static DailyUsageStatisticsResponse fromEntity(UsageDailyStatisticsEntity entity) {
        return new DailyUsageStatisticsResponse(entity.getDate(), entity.getTotalRequests(),
                entity.getPeakRequestsPerSecond(), entity.getAvgRequestsPerSecond(),
                entity.getRejectedRequests());
    }
}
