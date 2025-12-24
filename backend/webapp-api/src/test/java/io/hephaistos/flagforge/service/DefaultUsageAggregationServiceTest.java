package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class DefaultUsageAggregationServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UsageDailyStatisticsRepository statsRepository;

    @Mock
    private StatefulRedisConnection<String, String> redisConnection;

    @Mock
    private RedisCommands<String, String> redisCommands;

    private DefaultUsageAggregationService aggregationService;

    @BeforeEach
    void setUp() {
        when(redisConnection.sync()).thenReturn(redisCommands);
        aggregationService =
                new DefaultUsageAggregationService(environmentRepository, statsRepository,
                        redisConnection);
    }

    @Test
    void aggregateDailyStatisticsProcessesAllEnvironments() {
        UUID envId1 = UUID.randomUUID();
        UUID envId2 = UUID.randomUUID();
        var env1 = createEnvironmentEntity(envId1);
        var env2 = createEnvironmentEntity(envId2);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        when(environmentRepository.findAll()).thenReturn(List.of(env1, env2));
        when(statsRepository.findByEnvironmentIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(statsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aggregationService.aggregateDailyStatistics();

        ArgumentCaptor<UsageDailyStatisticsEntity> captor =
                ArgumentCaptor.forClass(UsageDailyStatisticsEntity.class);
        verify(statsRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        var savedEntities = captor.getAllValues();
        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities.stream()
                .map(UsageDailyStatisticsEntity::getEnvironmentId)).containsExactlyInAnyOrder(
                envId1, envId2);
    }

    @Test
    void aggregateDailyStatisticsReadsCorrectRedisKeys() {
        UUID envId = UUID.randomUUID();
        var env = createEnvironmentEntity(envId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String expectedDailyKey = "usage:daily:" + envId + ":" + today;
        String expectedPeakKey = "usage:peak:" + envId + ":" + today;

        when(environmentRepository.findAll()).thenReturn(List.of(env));
        when(redisCommands.get(expectedDailyKey)).thenReturn("1000");
        when(redisCommands.get(expectedPeakKey)).thenReturn("50");
        when(statsRepository.findByEnvironmentIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(statsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aggregationService.aggregateDailyStatistics();

        verify(redisCommands).get(expectedDailyKey);
        verify(redisCommands).get(expectedPeakKey);
    }

    @Test
    void aggregateDailyStatisticsSavesCorrectValues() {
        UUID envId = UUID.randomUUID();
        var env = createEnvironmentEntity(envId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        when(environmentRepository.findAll()).thenReturn(List.of(env));
        when(redisCommands.get("usage:daily:" + envId + ":" + today)).thenReturn("5000");
        when(redisCommands.get("usage:peak:" + envId + ":" + today)).thenReturn("75");
        when(statsRepository.findByEnvironmentIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(statsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aggregationService.aggregateDailyStatistics();

        ArgumentCaptor<UsageDailyStatisticsEntity> captor =
                ArgumentCaptor.forClass(UsageDailyStatisticsEntity.class);
        verify(statsRepository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getEnvironmentId()).isEqualTo(envId);
        assertThat(saved.getDate()).isEqualTo(today);
        assertThat(saved.getTotalRequests()).isEqualTo(5000L);
        assertThat(saved.getPeakRequestsPerSecond()).isEqualTo(75);
        assertThat(saved.getAvgRequestsPerSecond()).isNotNull();
    }

    @Test
    void aggregateDailyStatisticsHandlesMissingRedisData() {
        UUID envId = UUID.randomUUID();
        var env = createEnvironmentEntity(envId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        when(environmentRepository.findAll()).thenReturn(List.of(env));
        when(redisCommands.get(any())).thenReturn(null);
        when(statsRepository.findByEnvironmentIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(statsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aggregationService.aggregateDailyStatistics();

        ArgumentCaptor<UsageDailyStatisticsEntity> captor =
                ArgumentCaptor.forClass(UsageDailyStatisticsEntity.class);
        verify(statsRepository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getTotalRequests()).isZero();
        assertThat(saved.getPeakRequestsPerSecond()).isZero();
        // avgRps is 0 / secondsElapsed, which is 0.00
        assertThat(saved.getAvgRequestsPerSecond()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void aggregateDailyStatisticsUpdatesExistingRecord() {
        UUID envId = UUID.randomUUID();
        UUID statsId = UUID.randomUUID();
        var env = createEnvironmentEntity(envId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        var existingStats = new UsageDailyStatisticsEntity();
        existingStats.setId(statsId);
        existingStats.setEnvironmentId(envId);
        existingStats.setDate(today);
        existingStats.setTotalRequests(500L);
        existingStats.setPeakRequestsPerSecond(10);
        existingStats.setAvgRequestsPerSecond(BigDecimal.valueOf(0.5));

        when(environmentRepository.findAll()).thenReturn(List.of(env));
        when(redisCommands.get("usage:daily:" + envId + ":" + today)).thenReturn("2000");
        when(redisCommands.get("usage:peak:" + envId + ":" + today)).thenReturn("30");
        when(statsRepository.findByEnvironmentIdAndDate(envId, today)).thenReturn(
                Optional.of(existingStats));
        when(statsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aggregationService.aggregateDailyStatistics();

        ArgumentCaptor<UsageDailyStatisticsEntity> captor =
                ArgumentCaptor.forClass(UsageDailyStatisticsEntity.class);
        verify(statsRepository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(statsId); // Same record updated
        assertThat(saved.getTotalRequests()).isEqualTo(2000L);
        assertThat(saved.getPeakRequestsPerSecond()).isEqualTo(30);
    }

    @Test
    void aggregateDailyStatisticsContinuesOnSingleEnvironmentError() {
        UUID envId1 = UUID.randomUUID();
        UUID envId2 = UUID.randomUUID();
        var env1 = createEnvironmentEntity(envId1);
        var env2 = createEnvironmentEntity(envId2);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        when(environmentRepository.findAll()).thenReturn(List.of(env1, env2));
        // First environment throws exception
        when(redisCommands.get("usage:daily:" + envId1 + ":" + today)).thenThrow(
                new RuntimeException("Redis error"));
        // Second environment succeeds
        when(redisCommands.get("usage:daily:" + envId2 + ":" + today)).thenReturn("100");
        when(redisCommands.get("usage:peak:" + envId2 + ":" + today)).thenReturn("5");
        when(statsRepository.findByEnvironmentIdAndDate(envId2, today)).thenReturn(
                Optional.empty());
        when(statsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        aggregationService.aggregateDailyStatistics();

        // Should still save for the second environment
        ArgumentCaptor<UsageDailyStatisticsEntity> captor =
                ArgumentCaptor.forClass(UsageDailyStatisticsEntity.class);
        verify(statsRepository).save(captor.capture());
        assertThat(captor.getValue().getEnvironmentId()).isEqualTo(envId2);
    }

    @Test
    void aggregateDailyStatisticsDoesNothingWithNoEnvironments() {
        when(environmentRepository.findAll()).thenReturn(List.of());

        aggregationService.aggregateDailyStatistics();

        verify(statsRepository, never()).save(any());
    }

    private EnvironmentEntity createEnvironmentEntity(UUID id) {
        var entity = new EnvironmentEntity();
        entity.setId(id);
        entity.setName("Test Environment");
        entity.setApplicationId(UUID.randomUUID());
        return entity;
    }
}
