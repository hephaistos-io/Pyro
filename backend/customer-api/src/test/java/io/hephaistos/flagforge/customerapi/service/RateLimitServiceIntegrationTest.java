package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.customerapi.PostgresTestContainerConfiguration;
import io.hephaistos.flagforge.customerapi.RedisTestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({PostgresTestContainerConfiguration.class, RedisTestContainerConfiguration.class})
@Tag("integration")
class RateLimitServiceIntegrationTest {

    @Autowired
    private RateLimitService rateLimitService;

    private UUID testEnvironmentId;

    @BeforeEach
    void setUp() {
        // Use a new UUID for each test to avoid state pollution
        testEnvironmentId = UUID.randomUUID();
    }

    @Test
    void tryConsumeAllowsRequestsWithinLimit() {
        int rateLimit = 5;

        var result = rateLimitService.tryConsume(testEnvironmentId, rateLimit);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remainingTokens()).isEqualTo(rateLimit - 1);
    }

    @Test
    void tryConsumeTracksRemainingTokens() {
        int rateLimit = 3;

        var result1 = rateLimitService.tryConsume(testEnvironmentId, rateLimit);
        var result2 = rateLimitService.tryConsume(testEnvironmentId, rateLimit);
        var result3 = rateLimitService.tryConsume(testEnvironmentId, rateLimit);

        assertThat(result1.remainingTokens()).isEqualTo(2);
        assertThat(result2.remainingTokens()).isEqualTo(1);
        assertThat(result3.remainingTokens()).isEqualTo(0);
    }

    @Test
    void tryConsumeDeniesRequestsExceedingLimit() {
        int rateLimit = 2;

        // Consume all tokens
        rateLimitService.tryConsume(testEnvironmentId, rateLimit);
        rateLimitService.tryConsume(testEnvironmentId, rateLimit);

        // Third request should be denied
        var result = rateLimitService.tryConsume(testEnvironmentId, rateLimit);

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterMillis()).isGreaterThan(0);
    }

    @Test
    void incrementMonthlyUsageStartsAtOne() {
        long first = rateLimitService.incrementMonthlyUsage(testEnvironmentId);

        assertThat(first).isEqualTo(1);
    }

    @Test
    void incrementMonthlyUsageIncrementsCorrectly() {
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);
        long second = rateLimitService.incrementMonthlyUsage(testEnvironmentId);
        long third = rateLimitService.incrementMonthlyUsage(testEnvironmentId);

        assertThat(second).isEqualTo(2);
        assertThat(third).isEqualTo(3);
    }

    @Test
    void getMonthlyUsageReturnsZeroForNewEnvironment() {
        long usage = rateLimitService.getMonthlyUsage(testEnvironmentId);

        assertThat(usage).isEqualTo(0);
    }

    @Test
    void getMonthlyUsageReturnsCorrectValue() {
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);

        long usage = rateLimitService.getMonthlyUsage(testEnvironmentId);

        assertThat(usage).isEqualTo(2);
    }

    @Test
    void getRemainingMonthlyQuotaCalculatesCorrectly() {
        long limit = 1000;
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);

        long remaining = rateLimitService.getRemainingMonthlyQuota(testEnvironmentId, limit);

        assertThat(remaining).isEqualTo(998);
    }

    @Test
    void getRemainingMonthlyQuotaReturnsZeroWhenExceeded() {
        long limit = 2;
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);
        rateLimitService.incrementMonthlyUsage(testEnvironmentId);

        long remaining = rateLimitService.getRemainingMonthlyQuota(testEnvironmentId, limit);

        assertThat(remaining).isEqualTo(0);
    }

    @Test
    void differentEnvironmentsHaveIndependentCounters() {
        UUID env1 = UUID.randomUUID();
        UUID env2 = UUID.randomUUID();

        rateLimitService.incrementMonthlyUsage(env1);
        rateLimitService.incrementMonthlyUsage(env1);
        rateLimitService.incrementMonthlyUsage(env2);

        assertThat(rateLimitService.getMonthlyUsage(env1)).isEqualTo(2);
        assertThat(rateLimitService.getMonthlyUsage(env2)).isEqualTo(1);
    }

    @Test
    void differentEnvironmentsHaveIndependentRateLimits() {
        UUID env1 = UUID.randomUUID();
        UUID env2 = UUID.randomUUID();
        int rateLimit = 2;

        // Exhaust rate limit for env1
        rateLimitService.tryConsume(env1, rateLimit);
        rateLimitService.tryConsume(env1, rateLimit);
        var env1Result = rateLimitService.tryConsume(env1, rateLimit);

        // env2 should still have tokens
        var env2Result = rateLimitService.tryConsume(env2, rateLimit);

        assertThat(env1Result.allowed()).isFalse();
        assertThat(env2Result.allowed()).isTrue();
    }
}
