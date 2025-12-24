package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.customerapi.data.repository.TemplateRepository;
import io.hephaistos.flagforge.customerapi.data.repository.TemplateValuesRepository;
import io.hephaistos.flagforge.customerapi.data.repository.UserTemplateValuesRepository;
import io.hephaistos.flagforge.customerapi.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultTemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateValuesRepository templateValuesRepository;

    @Mock
    private UserTemplateValuesRepository userTemplateValuesRepository;

    private DefaultTemplateService templateService;
    private UUID applicationId;
    private UUID environmentId;

    @BeforeEach
    void setUp() {
        templateService = new DefaultTemplateService(templateRepository, templateValuesRepository,
                userTemplateValuesRepository);
        applicationId = UUID.randomUUID();
        environmentId = UUID.randomUUID();
    }

    @Test
    void getMergedSystemValuesReturnsDefaultsWhenNoIdentifier() {
        var template = createTemplateWithDefaults();
        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.of(template));

        var response = templateService.getMergedSystemValues(applicationId, environmentId, null);

        assertThat(response.type()).isEqualTo(TemplateType.SYSTEM);
        assertThat(response.values()).containsEntry("api_url", "https://default.api.com");
        assertThat(response.appliedIdentifier()).isNull();
    }

    @Test
    void getMergedSystemValuesReturnsDefaultsWhenBlankIdentifier() {
        var template = createTemplateWithDefaults();
        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.of(template));

        var response = templateService.getMergedSystemValues(applicationId, environmentId, "   ");

        assertThat(response.type()).isEqualTo(TemplateType.SYSTEM);
        assertThat(response.values()).containsEntry("api_url", "https://default.api.com");
        assertThat(response.appliedIdentifier()).isNull();
    }

    @Test
    void getMergedSystemValuesAppliesOverrideWhenIdentifierExists() {
        var template = createTemplateWithDefaults();
        var override = createOverride("region-eu", Map.of("api_url", "https://eu.api.com"));

        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                applicationId, environmentId, TemplateType.SYSTEM, "region-eu")).thenReturn(
                Optional.of(override));

        var response =
                templateService.getMergedSystemValues(applicationId, environmentId, "region-eu");

        assertThat(response.values()).containsEntry("api_url", "https://eu.api.com");
        assertThat(response.appliedIdentifier()).isEqualTo("region-eu");
    }

    @Test
    void getMergedSystemValuesReturnsDefaultsWhenIdentifierNotFound() {
        var template = createTemplateWithDefaults();

        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                applicationId, environmentId, TemplateType.SYSTEM, "non-existent")).thenReturn(
                Optional.empty());

        var response =
                templateService.getMergedSystemValues(applicationId, environmentId, "non-existent");

        assertThat(response.values()).containsEntry("api_url", "https://default.api.com");
        assertThat(response.appliedIdentifier()).isNull();
    }

    @Test
    void getMergedSystemValuesThrowsNotFoundWhenTemplateDoesNotExist() {
        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.getMergedSystemValues(applicationId, environmentId,
                null)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SYSTEM template not found");
    }

    @Test
    void getMergedSystemValuesIncludesSchemaInResponse() {
        var template = createTemplateWithDefaults();
        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.of(template));

        var response = templateService.getMergedSystemValues(applicationId, environmentId, null);

        assertThat(response.schema()).isNotNull();
        assertThat(response.schema().fields()).hasSize(1);
        assertThat(response.schema().fields().get(0).key()).isEqualTo("api_url");
    }

    @Test
    void getMergedSystemValuesPreservesNonOverriddenDefaults() {
        var schema = new TemplateSchema(List.of(new StringTemplateField("api_url", "API URL", false,
                        "https://default.api.com", 0, 255),
                new StringTemplateField("region", "Region", false, "us-east", 0, 50)));
        var template = new TemplateEntity();
        template.setId(UUID.randomUUID());
        template.setApplicationId(applicationId);
        template.setType(TemplateType.SYSTEM);
        template.setSchema(schema);

        // Override only api_url, not region
        var override = createOverride("custom", Map.of("api_url", "https://custom.api.com"));

        when(templateRepository.findByApplicationIdAndType(applicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                applicationId, environmentId, TemplateType.SYSTEM, "custom")).thenReturn(
                Optional.of(override));

        var response =
                templateService.getMergedSystemValues(applicationId, environmentId, "custom");

        // api_url should be overridden, region should keep default
        assertThat(response.values()).containsEntry("api_url", "https://custom.api.com");
        assertThat(response.values()).containsEntry("region", "us-east");
        assertThat(response.appliedIdentifier()).isEqualTo("custom");
    }

    private TemplateEntity createTemplateWithDefaults() {
        var schema = new TemplateSchema(List.of(new StringTemplateField("api_url", "API URL", false,
                "https://default.api.com", 0, 255)));
        var template = new TemplateEntity();
        template.setId(UUID.randomUUID());
        template.setApplicationId(applicationId);
        template.setType(TemplateType.SYSTEM);
        template.setSchema(schema);
        return template;
    }

    private TemplateValuesEntity createOverride(String identifier, Map<String, Object> values) {
        var entity = new TemplateValuesEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicationId(applicationId);
        entity.setEnvironmentId(environmentId);
        entity.setType(TemplateType.SYSTEM);
        entity.setIdentifier(identifier);
        entity.setValues(values);
        return entity;
    }
}
