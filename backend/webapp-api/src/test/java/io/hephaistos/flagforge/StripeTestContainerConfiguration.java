package io.hephaistos.flagforge;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Test configuration that provides a Stripe Mock Testcontainer for integration tests.
 * <p>
 * stripe-mock is an HTTP server that responds like the real Stripe API. It can be used instead of
 * Stripe's testmode to make test suites integrating with Stripe faster and less brittle.
 * <p>
 * The container is shared across all tests for performance.
 *
 * @see <a href="https://github.com/stripe/stripe-mock">stripe-mock on GitHub</a>
 */
@TestConfiguration(proxyBeanMethods = false)
public class StripeTestContainerConfiguration {

    private static final int HTTP_PORT = 12111;
    private static final int HTTPS_PORT = 12112;

    private static final GenericContainer<?> STRIPE_MOCK =
            new GenericContainer<>("stripe/stripe-mock:latest").withExposedPorts(HTTP_PORT,
                    HTTPS_PORT).waitingFor(Wait.forLogMessage(".*Listening for HTTP.*", 1));

    static {
        STRIPE_MOCK.start();
        // Set the mock URL as a system property BEFORE Spring starts
        // This ensures MockStripeService's @PostConstruct gets the right URL
        String mockUrl = getMockUrl();
        System.setProperty("flagforge.stripe.mock-base-url", mockUrl);
        System.setProperty("flagforge.stripe.mode", "mock");
    }

    private static String getMockUrl() {
        return String.format("http://%s:%d", STRIPE_MOCK.getHost(),
                STRIPE_MOCK.getMappedPort(HTTP_PORT));
    }

    @Bean
    public GenericContainer<?> stripeMockContainer() {
        return STRIPE_MOCK;
    }

    /**
     * Returns the base URL for the Stripe Mock API.
     *
     * @return the HTTP base URL (e.g., "http://localhost:32768")
     */
    @Bean
    public String stripeApiBaseUrl() {
        return getMockUrl();
    }
}
