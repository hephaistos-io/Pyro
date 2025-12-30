package io.hephaistos.flagforge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class DefaultEmailTemplateServiceTest {

    private DefaultEmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new DefaultEmailTemplateService();
    }

    @Test
    void processTemplate_loadsPasswordResetTemplate() {
        String result = templateService.processTemplate("password-reset.html",
                Map.of("ACTION_URL", "https://example.com/reset"));

        assertThat(result).contains("Reset Your Password");
        assertThat(result).contains("https://example.com/reset");
        assertThat(result).contains("Flagforge");
    }

    @Test
    void processTemplate_loadsEmailVerificationTemplate() {
        String result = templateService.processTemplate("email-verification.html",
                Map.of("ACTION_URL", "https://example.com/verify"));

        assertThat(result).contains("Verify Your New Email Address");
        assertThat(result).contains("https://example.com/verify");
        assertThat(result).contains("Flagforge");
    }

    @Test
    void processTemplate_loadsRegistrationVerificationTemplate() {
        String result = templateService.processTemplate("registration-verification.html",
                Map.of("ACTION_URL", "https://example.com/verify-registration"));

        assertThat(result).contains("Welcome to Flagforge");
        assertThat(result).contains("https://example.com/verify-registration");
    }

    @Test
    void processTemplate_replacesMultiplePlaceholders() {
        String result = templateService.processTemplate("password-reset.html",
                Map.of("ACTION_URL", "https://test.com/action"));

        // ACTION_URL appears twice in the template (button and plain text link)
        assertThat(result).doesNotContain("{{ACTION_URL}}");
        int count = result.split("https://test.com/action", -1).length - 1;
        assertThat(count).isEqualTo(2);
    }

    @Test
    void processTemplate_throwsOnMissingTemplate() {
        assertThatThrownBy(
                () -> templateService.processTemplate("nonexistent.html", Map.of())).isInstanceOf(
                        DefaultEmailTemplateService.EmailTemplateException.class)
                .hasMessageContaining("Failed to load email template");
    }

    @Test
    void processTemplate_preservesHtmlStructure() {
        String result = templateService.processTemplate("password-reset.html",
                Map.of("ACTION_URL", "https://example.com"));

        assertThat(result).contains("<!DOCTYPE html>");
        assertThat(result).contains("<html lang=\"en\">");
        assertThat(result).contains("</html>");
    }

    @Test
    void processTemplate_handlesEmptyVariables() {
        String result = templateService.processTemplate("password-reset.html", Map.of());

        // Template should still load, but placeholders remain
        assertThat(result).contains("{{ACTION_URL}}");
    }
}
