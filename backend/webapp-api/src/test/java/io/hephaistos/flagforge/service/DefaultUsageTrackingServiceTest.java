package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import io.hephaistos.flagforge.controller.dto.DailyUsageStatisticsResponse;
import io.hephaistos.flagforge.data.repository.UsageDailyStatisticsRepository;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class DefaultUsageTrackingServiceTest {

    @Mock
    private StatefulRedisConnection<String, String> redisConnection;

    @Mock
    private RedisCommands<String, String> redisCommands;

    @Mock
    private UsageDailyStatisticsRepository dailyStatsRepository;

    private DefaultUsageTrackingService usageTrackingService;

    @BeforeEach
    void setUp() {
        when(redisConnection.sync()).thenReturn(redisCommands);
        usageTrackingService =
                new DefaultUsageTrackingService(redisConnection, dailyStatsRepository);
    }

    // ========== getMonthlyUsage Tests ==========

    @Test
    void getMonthlyUsageReturnsValueFromRedis() {
        UUID envId = UUID.randomUUID();
        when(redisCommands.get(any())).thenReturn("12345");

        long result = usageTrackingService.getMonthlyUsage(envId);

        assertThat(result).isEqualTo(12345L);
    }

    @Test
    void getMonthlyUsageReturnsZeroWhenKeyNotFound() {
        UUID envId = UUID.randomUUID();
        when(redisCommands.get(any())).thenReturn(null);

        long result = usageTrackingService.getMonthlyUsage(envId);

        assertThat(result).isZero();
    }

    @Test
    void getMonthlyUsageReturnsZeroOnRedisError() {
        UUID envId = UUID.randomUUID();
        when(redisCommands.get(any())).thenThrow(new RuntimeException("Redis error"));

        long result = usageTrackingService.getMonthlyUsage(envId);

        assertThat(result).isZero();
    }

    // ========== getDailyStatistics Tests ==========

    @Test
    void getDailyStatisticsReturnsCorrectNumberOfDays() {
        UUID envId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = today.minusDays(6); // 7 days including today

        var entity1 = createStatsEntity(envId, today, 1000, 50, BigDecimal.valueOf(0.5));
        var entity2 =
                createStatsEntity(envId, today.minusDays(1), 900, 45, BigDecimal.valueOf(0.4));

        when(dailyStatsRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                eq(envId), eq(startDate))).thenReturn(List.of(entity1, entity2));

        List<DailyUsageStatisticsResponse> result =
                usageTrackingService.getDailyStatistics(envId, 7);

        assertThat(result).hasSize(2);
    }

    @Test
    void getDailyStatisticsCalculatesCorrectStartDate() {
        UUID envId = UUID.randomUUID();

        usageTrackingService.getDailyStatistics(envId, 7);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyStatsRepository).findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                eq(envId), dateCaptor.capture());

        LocalDate expectedStartDate = LocalDate.now(ZoneOffset.UTC).minusDays(6);
        assertThat(dateCaptor.getValue()).isEqualTo(expectedStartDate);
    }

    @Test
    void getDailyStatisticsConvertsEntitiesToDTOs() {
        UUID envId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        var entity = createStatsEntity(envId, today, 5000, 100, BigDecimal.valueOf(1.5));

        when(dailyStatsRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(any(),
                any())).thenReturn(List.of(entity));

        List<DailyUsageStatisticsResponse> result =
                usageTrackingService.getDailyStatistics(envId, 7);

        assertThat(result).hasSize(1);
        var dto = result.get(0);
        assertThat(dto.date()).isEqualTo(today);
        assertThat(dto.totalRequests()).isEqualTo(5000);
        assertThat(dto.peakRequestsPerSecond()).isEqualTo(100);
        assertThat(dto.avgRequestsPerSecond()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
    }

    @Test
    void getDailyStatisticsReturnsEmptyListWhenNoData() {
        UUID envId = UUID.randomUUID();

        when(dailyStatsRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(any(),
                any())).thenReturn(List.of());

        List<DailyUsageStatisticsResponse> result =
                usageTrackingService.getDailyStatistics(envId, 7);

        assertThat(result).isEmpty();
    }

    @Test
    void getDailyStatisticsPreservesOrderFromRepository() {
        UUID envId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        var entity1 = createStatsEntity(envId, today, 1000, 50, BigDecimal.ONE);
        var entity2 = createStatsEntity(envId, today.minusDays(1), 900, 45, BigDecimal.ONE);
        var entity3 = createStatsEntity(envId, today.minusDays(2), 800, 40, BigDecimal.ONE);

        when(dailyStatsRepository.findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(any(),
                any())).thenReturn(List.of(entity1, entity2, entity3));

        List<DailyUsageStatisticsResponse> result =
                usageTrackingService.getDailyStatistics(envId, 7);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).date()).isEqualTo(today);
        assertThat(result.get(1).date()).isEqualTo(today.minusDays(1));
        assertThat(result.get(2).date()).isEqualTo(today.minusDays(2));
    }

    @Test
    void getDailyStatisticsHandles30DayRange() {
        UUID envId = UUID.randomUUID();

        usageTrackingService.getDailyStatistics(envId, 30);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyStatsRepository).findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
                eq(envId), dateCaptor.capture());

        LocalDate expectedStartDate = LocalDate.now(ZoneOffset.UTC).minusDays(29);
        assertThat(dateCaptor.getValue()).isEqualTo(expectedStartDate);
    }

    private UsageDailyStatisticsEntity createStatsEntity(UUID envId, LocalDate date,
            long totalRequests, int peakRps, BigDecimal avgRps) {
        var entity = new UsageDailyStatisticsEntity();
        entity.setId(UUID.randomUUID());
        entity.setEnvironmentId(envId);
        entity.setDate(date);
        entity.setTotalRequests(totalRequests);
        entity.setPeakRequestsPerSecond(peakRps);
        entity.setAvgRequestsPerSecond(avgRps);
        return entity;
    }
}
