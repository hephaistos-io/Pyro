package io.hephaistos.flagforge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Default implementation of EmailTemplateService that loads templates from the classpath. Templates
 * are stored in resources/templates/email/ and use {{PLACEHOLDER}} syntax.
 */
@Service
public class DefaultEmailTemplateService implements EmailTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEmailTemplateService.class);
    private static final String TEMPLATE_PATH = "templates/email/";

    @Override
    public String processTemplate(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        return replacePlaceholders(template, variables);
    }

    private String loadTemplate(String templateName) {
        String resourcePath = TEMPLATE_PATH + templateName;
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        catch (IOException e) {
            LOGGER.error("Failed to load email template: {}", resourcePath, e);
            throw new EmailTemplateException("Failed to load email template: " + templateName, e);
        }
    }

    private String replacePlaceholders(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    /**
     * Exception thrown when email template loading or processing fails.
     */
    public static class EmailTemplateException extends RuntimeException {
        public EmailTemplateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
