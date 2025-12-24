package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.UsageDailyStatisticsRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Service for aggregating usage statistics from Redis into the database. Runs hourly to consolidate
 * daily usage data for historical queries.
 */
@Service
@ConditionalOnProperty(name = "flagforge.redis.enabled", havingValue = "true",
        matchIfMissing = true)
public class DefaultUsageAggregationService implements UsageAggregationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefaultUsageAggregationService.class);
    private static final String USAGE_DAILY_KEY_PREFIX = "usage:daily:";
    private static final String USAGE_PEAK_KEY_PREFIX = "usage:peak:";
    private static final String USAGE_REJECTED_KEY_PREFIX = "usage:rejected:";

    private final EnvironmentRepository environmentRepository;
    private final UsageDailyStatisticsRepository statsRepository;
    private final StatefulRedisConnection<String, String> redisConnection;

    public DefaultUsageAggregationService(EnvironmentRepository environmentRepository,
            UsageDailyStatisticsRepository statsRepository,
            StatefulRedisConnection<String, String> redisConnection) {
        this.environmentRepository = environmentRepository;
        this.statsRepository = statsRepository;
        this.redisConnection = redisConnection;
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    @Transactional
    public void aggregateDailyStatistics() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LOGGER.info("Starting daily statistics aggregation for {}", today);

        List<EnvironmentEntity> environments = environmentRepository.findAll();
        int successCount = 0;
        int errorCount = 0;

        for (EnvironmentEntity env : environments) {
            try {
                aggregateForEnvironment(env.getId(), today);
                successCount++;
            }
            catch (Exception e) {
                LOGGER.error("Failed to aggregate stats for environment {}", env.getId(), e);
                errorCount++;
            }
        }

        LOGGER.info("Completed daily statistics aggregation: {} succeeded, {} failed", successCount,
                errorCount);
    }

    private void aggregateForEnvironment(UUID envId, LocalDate date) {
        var commands = redisConnection.sync();

        // Read daily total from Redis
        String dailyKey = USAGE_DAILY_KEY_PREFIX + envId + ":" + date;
        String dailyValue = commands.get(dailyKey);
        long totalRequests = dailyValue != null ? Long.parseLong(dailyValue) : 0;

        // Read peak burst from Redis
        String peakKey = USAGE_PEAK_KEY_PREFIX + envId + ":" + date;
        String peakValue = commands.get(peakKey);
        int peakRps = peakValue != null ? Integer.parseInt(peakValue) : 0;

        // Read rejected requests from Redis
        String rejectedKey = USAGE_REJECTED_KEY_PREFIX + envId + ":" + date;
        String rejectedValue = commands.get(rejectedKey);
        long rejectedRequests = rejectedValue != null ? Long.parseLong(rejectedValue) : 0;

        // Calculate average req/sec (total / seconds elapsed in day so far)
        long secondsElapsed = calculateSecondsElapsedToday(date);
        BigDecimal avgRps = secondsElapsed > 0 ?
                BigDecimal.valueOf(totalRequests)
                        .divide(BigDecimal.valueOf(secondsElapsed), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Upsert to database
        UsageDailyStatisticsEntity stats =
                statsRepository.findByEnvironmentIdAndDate(envId, date).orElseGet(() -> {
                    var entity = new UsageDailyStatisticsEntity();
                    entity.setEnvironmentId(envId);
                    entity.setDate(date);
                    return entity;
                });

        stats.setTotalRequests(totalRequests);
        stats.setPeakRequestsPerSecond(peakRps);
        stats.setAvgRequestsPerSecond(avgRps);
        stats.setRejectedRequests(rejectedRequests);

        statsRepository.save(stats);

        LOGGER.debug(
                "Aggregated stats for environment {}: {} requests, {} peak rps, {} avg rps, {} rejected",
                envId, totalRequests, peakRps, avgRps, rejectedRequests);
    }

    private long calculateSecondsElapsedToday(LocalDate date) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (date.isBefore(today)) {
            // Past day - full 24 hours
            return Duration.ofDays(1).toSeconds();
        }
        else if (date.isEqual(today)) {
            // Current day - seconds elapsed since midnight UTC
            LocalTime nowUtc = LocalTime.now(ZoneOffset.UTC);
            return nowUtc.toSecondOfDay();
        }
        else {
            // Future date (shouldn't happen, but handle gracefully)
            return 0;
        }
    }
}
