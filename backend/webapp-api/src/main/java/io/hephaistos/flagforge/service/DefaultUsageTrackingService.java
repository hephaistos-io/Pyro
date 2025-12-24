package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.DailyUsageStatisticsResponse;
import io.hephaistos.flagforge.data.repository.UsageDailyStatisticsRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Redis-based implementation of UsageTrackingService. Reads monthly usage counters from Redis and
 * daily statistics from database.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "true",
        matchIfMissing = true)
public class DefaultUsageTrackingService implements UsageTrackingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUsageTrackingService.class);
    private static final String USAGE_KEY_PREFIX = "usage:monthly:";

    private final StatefulRedisConnection<String, String> redisConnection;
    private final UsageDailyStatisticsRepository dailyStatsRepository;

    public DefaultUsageTrackingService(StatefulRedisConnection<String, String> redisConnection,
            UsageDailyStatisticsRepository dailyStatsRepository) {
        this.redisConnection = redisConnection;
        this.dailyStatsRepository = dailyStatsRepository;
    }

    @Override
    public long getMonthlyUsage(UUID environmentId) {
        String key = getMonthlyUsageKey(environmentId);
        try {
            String value = redisConnection.sync().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        }
        catch (Exception e) {
            LOGGER.error("Failed to get monthly usage for environment {}", environmentId, e);
            return 0L;
        }
    }

    @Override
    public List<DailyUsageStatisticsResponse> getDailyStatistics(UUID environmentId, int days) {
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1);
        return dailyStatsRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                        environmentId, startDate)
                .stream()
                .map(DailyUsageStatisticsResponse::fromEntity)
                .toList();
    }

    private String getMonthlyUsageKey(UUID environmentId) {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        return USAGE_KEY_PREFIX + environmentId + ":" + currentMonth;
    }
}
