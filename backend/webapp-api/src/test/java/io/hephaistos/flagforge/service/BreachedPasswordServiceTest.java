package io.hephaistos.flagforge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BreachedPasswordServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    private BreachedPasswordService breachedPasswordService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        breachedPasswordService = new BreachedPasswordService(restClientBuilder);
    }

    @Test
    void isPasswordBreachedReturnsTrueWhenPasswordFound() {
        // "password" has SHA-1 hash: 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
        // Prefix: 5BAA6, Suffix: 1E4C9B93F3F0682250B6CF8331B7EE68FD8
        String apiResponse = """
                1E4C9B93F3F0682250B6CF8331B7EE68FD8:123456
                2E23E3A3F2E4B5C6D7E8F9A0B1C2D3E4F5G:100
                """;

        setupRestClientMock(apiResponse);

        boolean result = breachedPasswordService.isPasswordBreached("password");

        assertThat(result).isTrue();
    }

    @Test
    void isPasswordBreachedReturnsFalseWhenPasswordNotFound() {
        String apiResponse = """
                AAAABBBBCCCCDDDDEEEEFFFFEEEEDDDDCCCC:123456
                FFFFEEEEDDDDCCCCBBBBAAAA0000111122:100
                """;

        setupRestClientMock(apiResponse);

        boolean result = breachedPasswordService.isPasswordBreached("veryUniquePassword123!@#");

        assertThat(result).isFalse();
    }

    @Test
    void isPasswordBreachedReturnsFalseWhenApiReturnsNull() {
        setupRestClientMock(null);

        boolean result = breachedPasswordService.isPasswordBreached("anyPassword");

        assertThat(result).isFalse();
    }

    @Test
    void isPasswordBreachedReturnsFalseWhenApiThrowsException() {
        when(restClient.get()).thenThrow(new RestClientException("API unavailable"));

        boolean result = breachedPasswordService.isPasswordBreached("anyPassword");

        // Should fail open - don't block registration if API is down
        assertThat(result).isFalse();
    }

    @Test
    void isPasswordBreachedHandlesEmptyApiResponse() {
        setupRestClientMock("");

        boolean result = breachedPasswordService.isPasswordBreached("anyPassword");

        assertThat(result).isFalse();
    }

    private void setupRestClientMock(String responseBody) {
        when(restClient.get().uri(any(String.class)).retrieve().body(String.class)).thenReturn(
                responseBody);
    }
}
