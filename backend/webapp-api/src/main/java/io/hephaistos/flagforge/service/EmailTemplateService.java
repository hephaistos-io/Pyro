package io.hephaistos.flagforge.service;

import java.util.Map;

/**
 * Service for loading and processing email HTML templates. Templates are loaded from the classpath
 * and placeholders are replaced with provided values.
 */
public interface EmailTemplateService {

    /**
     * Load and process an email template.
     *
     * @param templateName the name of the template file (without path, e.g.,
     *                     "password-reset.html")
     * @param variables    a map of placeholder names to values (e.g., "ACTION_URL" ->
     *                     "https://...")
     * @return the processed HTML content with placeholders replaced
     */
    String processTemplate(String templateName, Map<String, String> variables);
}
