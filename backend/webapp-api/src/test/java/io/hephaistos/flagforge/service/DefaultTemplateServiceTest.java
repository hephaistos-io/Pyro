package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.cache.CacheInvalidationPublisher;
import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.common.data.TemplateEntity;
import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.types.BooleanTemplateField;
import io.hephaistos.flagforge.common.types.EnumTemplateField;
import io.hephaistos.flagforge.common.types.NumberTemplateField;
import io.hephaistos.flagforge.common.types.StringTemplateField;
import io.hephaistos.flagforge.common.types.TemplateSchema;
import io.hephaistos.flagforge.controller.dto.CopyOverridesRequest;
import io.hephaistos.flagforge.controller.dto.TemplateUpdateRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesRequest;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.TemplateRepository;
import io.hephaistos.flagforge.data.repository.TemplateValuesRepository;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultTemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateValuesRepository templateValuesRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private CacheInvalidationPublisher cacheInvalidationPublisher;

    private DefaultTemplateService templateService;
    private UUID testCompanyId;
    private UUID testApplicationId;
    private UUID testEnvironmentId;

    @BeforeEach
    void setUp() {
        templateService = new DefaultTemplateService(templateRepository, templateValuesRepository,
                applicationRepository, environmentRepository, cacheInvalidationPublisher);
        testCompanyId = UUID.randomUUID();
        UUID testCustomerId = UUID.randomUUID();
        testApplicationId = UUID.randomUUID();
        testEnvironmentId = UUID.randomUUID();
        setupSecurityContext(testCompanyId, testCustomerId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== createDefaultTemplates Tests ==========

    @Test
    void createDefaultTemplatesCreatesBothTypes() {
        var application = createApplicationEntity();

        templateService.createDefaultTemplates(application);

        // Should add exactly 2 templates (USER and SYSTEM) to the application's collection
        assertThat(application.getTemplates()).hasSize(2);

        // Verify both types are created
        assertThat(application.getTemplates()).extracting(TemplateEntity::getType)
                .containsExactlyInAnyOrder(TemplateType.USER, TemplateType.SYSTEM);

        // Verify all templates have correct company ID and back-reference
        for (var template : application.getTemplates()) {
            assertThat(template.getApplication()).isSameAs(application);
            assertThat(template.getApplicationId()).isEqualTo(testApplicationId);
            assertThat(template.getCompanyId()).isEqualTo(testCompanyId);
            assertThat(template.getSchema().fields()).isEmpty();
        }
    }

    // ========== getTemplates Tests ==========

    @Test
    void getTemplatesReturnsListSuccessfully() {
        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(templateRepository.findByApplicationId(testApplicationId)).thenReturn(
                List.of(createTemplateEntity(TemplateType.USER)));

        var templates = templateService.getTemplates(testApplicationId);

        assertThat(templates).hasSize(1);
        assertThat(templates.getFirst().type()).isEqualTo(TemplateType.USER);
    }

    @Test
    void getTemplatesThrowsNotFoundForInvalidApplication() {
        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(false);

        assertThatThrownBy(() -> templateService.getTemplates(testApplicationId)).isInstanceOf(
                NotFoundException.class);
    }

    // ========== getTemplateByType Tests ==========

    @Test
    void getTemplateByTypeSuccessfully() {
        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(
                Optional.of(createTemplateEntity(TemplateType.USER)));

        var template = templateService.getTemplateByType(testApplicationId, TemplateType.USER);

        assertThat(template.type()).isEqualTo(TemplateType.USER);
    }

    @Test
    void getTemplateByTypeThrowsNotFoundException() {
        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.SYSTEM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.getTemplateByType(testApplicationId,
                TemplateType.SYSTEM)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("SYSTEM");
    }

    // ========== updateTemplate Tests ==========

    @Test
    void updateTemplateSuccessfully() {
        var existingTemplate = createTemplateEntity(TemplateType.USER);
        var newSchema = new TemplateSchema(
                List.of(new StringTemplateField("updated_field", "Updated", true, "default", 0,
                        255)));
        var request = new TemplateUpdateRequest(newSchema);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(i -> i.getArgument(0));

        var response =
                templateService.updateTemplate(testApplicationId, TemplateType.USER, request);

        assertThat(response.schema().fields()).hasSize(1);
        assertThat(response.schema().fields().getFirst().key()).isEqualTo("updated_field");
    }

    // ========== getMergedValues Tests ==========

    @Test
    void getMergedValuesReturnsDefaultsOnly() {
        var template = createTemplateEntity(TemplateType.USER);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        var response = templateService.getMergedValues(testApplicationId, testEnvironmentId,
                TemplateType.USER, null);

        assertThat(response.values()).containsEntry("test_key", "test_default");
        assertThat(response.appliedIdentifiers()).isEmpty();
    }

    @Test
    void getMergedValuesAppliesIdentifiersInOrder() {
        var template = createTemplateEntity(TemplateType.USER);
        var override1 = createValuesEntity("region-eu", Map.of("test_key", "eu_value"));
        var override2 = createValuesEntity("user-123", Map.of("test_key", "user_value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifierIn(
                testApplicationId, testEnvironmentId, TemplateType.USER,
                List.of("region-eu", "user-123"))).thenReturn(List.of(override1, override2));

        var response = templateService.getMergedValues(testApplicationId, testEnvironmentId,
                TemplateType.USER, List.of("region-eu", "user-123"));

        // User override should win (last in list)
        assertThat(response.values()).containsEntry("test_key", "user_value");
        assertThat(response.appliedIdentifiers()).containsExactly("region-eu", "user-123");
    }

    @Test
    void getMergedValuesSkipsMissingIdentifiers() {
        var template = createTemplateEntity(TemplateType.USER);
        var override = createValuesEntity("region-eu", Map.of("test_key", "eu_value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifierIn(
                testApplicationId, testEnvironmentId, TemplateType.USER,
                List.of("region-eu", "missing"))).thenReturn(List.of(override));

        var response = templateService.getMergedValues(testApplicationId, testEnvironmentId,
                TemplateType.USER, List.of("region-eu", "missing"));

        assertThat(response.values()).containsEntry("test_key", "eu_value");
        assertThat(response.appliedIdentifiers()).containsExactly("region-eu");
    }

    // ========== setOverride Tests ==========

    @Test
    void setOverrideCreatesNewOverride() {
        var template = createTemplateEntity(TemplateType.USER);
        var request = new TemplateValuesRequest(Map.of("test_key", "override_value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, testEnvironmentId, TemplateType.USER, "user-123")).thenReturn(
                Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                invocation -> {
                    TemplateValuesEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        var response =
                templateService.setOverride(testApplicationId, testEnvironmentId, TemplateType.USER,
                        "user-123", request);

        assertThat(response.identifier()).isEqualTo("user-123");
        assertThat(response.values()).containsEntry("test_key", "override_value");

        ArgumentCaptor<TemplateValuesEntity> captor =
                ArgumentCaptor.forClass(TemplateValuesEntity.class);
        verify(templateValuesRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(testApplicationId);
        assertThat(captor.getValue().getType()).isEqualTo(TemplateType.USER);
    }

    @Test
    void setOverrideUpdatesExistingOverride() {
        var template = createTemplateEntity(TemplateType.USER);
        var existingOverride = createValuesEntity("user-123", Map.of("old_key", "old_value"));
        var request = new TemplateValuesRequest(Map.of("new_key", "new_value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, testEnvironmentId, TemplateType.USER, "user-123")).thenReturn(
                Optional.of(existingOverride));
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        templateService.setOverride(testApplicationId, testEnvironmentId, TemplateType.USER,
                "user-123", request);

        ArgumentCaptor<TemplateValuesEntity> captor =
                ArgumentCaptor.forClass(TemplateValuesEntity.class);
        verify(templateValuesRepository).save(captor.capture());
        assertThat(captor.getValue().getValues()).containsEntry("new_key", "new_value");
    }

    @Test
    void setOverrideThrowsNotFoundWhenTemplateDoesNotExist() {
        var request = new TemplateValuesRequest(Map.of("test_key", "value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("USER");

        verify(templateValuesRepository, never()).save(any());
    }

    // ========== deleteOverride Tests ==========

    @Test
    void deleteOverrideSuccessfully() {
        var override = createValuesEntity("user-123", Map.of("key", "value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, testEnvironmentId, TemplateType.USER, "user-123")).thenReturn(
                Optional.of(override));

        templateService.deleteOverride(testApplicationId, testEnvironmentId, TemplateType.USER,
                "user-123");

        verify(templateValuesRepository).delete(override);
    }

    @Test
    void deleteOverrideThrowsNotFoundForMissingIdentifier() {
        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, testEnvironmentId, TemplateType.USER, "missing")).thenReturn(
                Optional.empty());

        assertThatThrownBy(
                () -> templateService.deleteOverride(testApplicationId, testEnvironmentId,
                        TemplateType.USER, "missing")).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ========== setOverride Validation Tests ==========

    @Test
    void setOverrideRejectsStringValueTooShort() {
        // Template schema requires minLength=5 for "username" field
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("username", "Username", true, "admin", 5, 50)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a value that's too short (3 chars < 5 minLength)
        var request = new TemplateValuesRequest(Map.of("username", "abc"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("minLength")
                .hasMessageContaining("5");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsStringValueTooLong() {
        // Template schema requires maxLength=10 for "code" field
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("code", "Code", true, "default", 0, 10)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a value that's too long (15 chars > 10 maxLength)
        var request = new TemplateValuesRequest(Map.of("code", "this_is_too_long"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("maxLength")
                .hasMessageContaining("10");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsNumberValueBelowMin() {
        // Template schema requires minValue=10 for "count" field
        var schema = new TemplateSchema(
                List.of(new NumberTemplateField("count", "Count", true, 10.0, 10.0, 100.0, 1.0)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a value below minimum (5 < 10)
        var request = new TemplateValuesRequest(Map.of("count", 5));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("minValue")
                .hasMessageContaining("10");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsNumberValueAboveMax() {
        // Template schema requires maxValue=100 for "percentage" field
        var schema = new TemplateSchema(
                List.of(new NumberTemplateField("percentage", "Percentage", true, 50.0, 0.0, 100.0,
                        1.0)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a value above maximum (150 > 100)
        var request = new TemplateValuesRequest(Map.of("percentage", 150));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("maxValue")
                .hasMessageContaining("100");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsNumberValueNotAlignedWithIncrement() {
        // Template schema requires increment of 5, starting from min=0
        var schema = new TemplateSchema(
                List.of(new NumberTemplateField("step", "Step", true, 10.0, 0.0, 100.0, 5.0)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a value not aligned with increment (12 is not divisible by 5 from 0)
        var request = new TemplateValuesRequest(Map.of("step", 12));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("incrementAmount");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsEnumValueNotInOptions() {
        // Template schema with enum field having specific options
        var schema = new TemplateSchema(
                List.of(new EnumTemplateField("region", "Region", true, "us-east",
                        List.of("us-east", "us-west", "eu-central"))));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a value not in the options list
        var request = new TemplateValuesRequest(Map.of("region", "asia-pacific"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("region")
                .hasMessageContaining("options");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsBooleanFieldWithNonBooleanValue() {
        // Template schema with boolean field
        var schema = new TemplateSchema(
                List.of(new BooleanTemplateField("enabled", "Enabled", true, false)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a non-boolean value (string instead of boolean)
        var request = new TemplateValuesRequest(Map.of("enabled", "yes"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("boolean");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsWrongTypeForStringField() {
        // Template schema with string field
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("name", "Name", true, "default", 1, 100)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a number value for a string field
        var request = new TemplateValuesRequest(Map.of("name", 12345));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("String");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideRejectsWrongTypeForNumberField() {
        // Template schema with number field
        var schema = new TemplateSchema(
                List.of(new NumberTemplateField("count", "Count", true, 10.0, 0.0, 100.0, 1.0)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Attempt to set a string value for a number field
        var request = new TemplateValuesRequest(Map.of("count", "fifty"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> templateService.setOverride(testApplicationId, testEnvironmentId,
                TemplateType.USER, "user-123", request)).isInstanceOf(
                IllegalArgumentException.class).hasMessageContaining("Number");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void setOverrideAcceptsValidValues() {
        // Template schema with multiple field types
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("name", "Name", true, "default", 3, 50),
                        new NumberTemplateField("count", "Count", true, 10.0, 0.0, 100.0, 5.0),
                        new EnumTemplateField("region", "Region", true, "us-east",
                                List.of("us-east", "us-west")),
                        new BooleanTemplateField("enabled", "Enabled", true, false)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // All values are valid
        var request = new TemplateValuesRequest(
                Map.of("name", "valid_name", "count", 15, "region", "us-west", "enabled", true));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, testEnvironmentId, TemplateType.USER, "user-123")).thenReturn(
                Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                invocation -> {
                    TemplateValuesEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        var response =
                templateService.setOverride(testApplicationId, testEnvironmentId, TemplateType.USER,
                        "user-123", request);

        assertThat(response.values()).containsEntry("name", "valid_name");
        assertThat(response.values()).containsEntry("count", 15);
        assertThat(response.values()).containsEntry("region", "us-west");
        assertThat(response.values()).containsEntry("enabled", true);
        verify(templateValuesRepository).save(any());
    }

    @Test
    void setOverrideAllowsPartialOverride() {
        // Template schema with multiple fields
        var schema = new TemplateSchema(
                List.of(new StringTemplateField("name", "Name", true, "default", 0, 50),
                        new NumberTemplateField("count", "Count", true, 10.0, 0.0, 100.0, 1.0)));
        var template = createTemplateEntityWithSchema(TemplateType.USER, schema);

        // Only override one field (partial override)
        var request = new TemplateValuesRequest(Map.of("name", "custom"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.findByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(Optional.of(template));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, testEnvironmentId, TemplateType.USER, "user-123")).thenReturn(
                Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                invocation -> {
                    TemplateValuesEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        var response =
                templateService.setOverride(testApplicationId, testEnvironmentId, TemplateType.USER,
                        "user-123", request);

        assertThat(response.values()).containsEntry("name", "custom");
        assertThat(response.values()).doesNotContainKey("count"); // Other fields not overridden
        verify(templateValuesRepository).save(any());
    }

    // ========== copyOverrides Tests ==========

    @Test
    void copyOverridesCopiesToEmptyEnvironment() {
        // Setup: Source environment has overrides, target is empty
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var userOverride1 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice"));
        var userOverride2 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-456",
                Map.of("name", "Bob"));
        var systemOverride = createValuesEntityForEnv(sourceEnvId, TemplateType.SYSTEM, "region-eu",
                Map.of("region", "europe"));

        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, null, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.USER)).thenReturn(List.of(userOverride1, userOverride2));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.SYSTEM)).thenReturn(List.of(systemOverride));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(any(),
                eq(targetEnvId), any(), any())).thenReturn(Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(3); // 2 USER + 1 SYSTEM
        assertThat(response.skippedCount()).isEqualTo(0);
        verify(templateValuesRepository, times(3)).save(any());
    }

    @Test
    void copyOverridesWithTypeFilterCopiesOnlySpecifiedType() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var userOverride = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice"));
        var systemOverride = createValuesEntityForEnv(sourceEnvId, TemplateType.SYSTEM, "region-eu",
                Map.of("region", "europe"));

        // Request to copy only USER type
        var request =
                new CopyOverridesRequest(sourceEnvId, targetEnvId, List.of(TemplateType.USER), null,
                        null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.USER)).thenReturn(List.of(userOverride));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(any(),
                eq(targetEnvId), any(), any())).thenReturn(Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(1); // Only USER
        assertThat(response.skippedCount()).isEqualTo(0);
        verify(templateValuesRepository).save(any());
        // Verify SYSTEM type was never queried
        verify(templateValuesRepository, never()).findByApplicationIdAndEnvironmentIdAndType(
                testApplicationId, sourceEnvId, TemplateType.SYSTEM);
    }

    @Test
    void copyOverridesWithIdentifierFilterCopiesOnlySpecifiedIdentifiers() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var override1 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice"));
        var override2 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-456",
                Map.of("name", "Bob"));
        var override3 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-789",
                Map.of("name", "Charlie"));

        // Request to copy only user-123 and user-789
        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, null,
                List.of("user-123", "user-789"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.USER)).thenReturn(
                List.of(override1, override2, override3));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.SYSTEM)).thenReturn(List.of());
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(any(),
                eq(targetEnvId), any(), any())).thenReturn(Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(2); // user-123 and user-789
        assertThat(response.skippedCount()).isEqualTo(0);
        verify(templateValuesRepository, times(2)).save(any());
    }

    @Test
    void copyOverridesWithOverwriteFalseSkipsExisting() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var sourceOverride1 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice-Source"));
        var sourceOverride2 = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-456",
                Map.of("name", "Bob-Source"));

        var existingTarget = createValuesEntityForEnv(targetEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice-Target"));

        // Request with overwrite=false (should skip user-123)
        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, false, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.USER)).thenReturn(
                List.of(sourceOverride1, sourceOverride2));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.SYSTEM)).thenReturn(List.of());
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, targetEnvId, TemplateType.USER, "user-123")).thenReturn(
                Optional.of(existingTarget));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, targetEnvId, TemplateType.USER, "user-456")).thenReturn(
                Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(1); // Only user-456
        assertThat(response.skippedCount()).isEqualTo(1); // user-123 skipped
        verify(templateValuesRepository, times(1)).save(any());
    }

    @Test
    void copyOverridesWithOverwriteTrueOverwritesExisting() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var sourceOverride = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice-Source", "email", "alice@source.com"));

        var existingTarget = createValuesEntityForEnv(targetEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice-Target"));

        // Request with overwrite=true
        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, true, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.USER)).thenReturn(List.of(sourceOverride));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.SYSTEM)).thenReturn(List.of());
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                testApplicationId, targetEnvId, TemplateType.USER, "user-123")).thenReturn(
                Optional.of(existingTarget));
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);

        // Verify the existing entity was updated
        ArgumentCaptor<TemplateValuesEntity> captor =
                ArgumentCaptor.forClass(TemplateValuesEntity.class);
        verify(templateValuesRepository).save(captor.capture());
        assertThat(captor.getValue().getValues()).containsEntry("name", "Alice-Source");
        assertThat(captor.getValue().getValues()).containsEntry("email", "alice@source.com");
    }

    @Test
    void copyOverridesCombinedFiltersTypesAndIdentifiers() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var userOverride = createValuesEntityForEnv(sourceEnvId, TemplateType.USER, "user-123",
                Map.of("name", "Alice"));
        var systemOverride = createValuesEntityForEnv(sourceEnvId, TemplateType.SYSTEM, "region-eu",
                Map.of("region", "europe"));

        // Request to copy only SYSTEM type AND only region-eu identifier
        var request =
                new CopyOverridesRequest(sourceEnvId, targetEnvId, List.of(TemplateType.SYSTEM),
                        null, List.of("region-eu"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(testApplicationId,
                sourceEnvId, TemplateType.SYSTEM)).thenReturn(List.of(systemOverride));
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(any(),
                eq(targetEnvId), any(), any())).thenReturn(Optional.empty());
        when(templateValuesRepository.save(any(TemplateValuesEntity.class))).thenAnswer(
                i -> i.getArgument(0));

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);
        // Verify USER type was never queried
        verify(templateValuesRepository, never()).findByApplicationIdAndEnvironmentIdAndType(
                testApplicationId, sourceEnvId, TemplateType.USER);
    }

    @Test
    void copyOverridesReturnsZeroWhenNoOverridesInSource() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();

        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, null, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(true);
        when(templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(any(),
                eq(sourceEnvId), any())).thenReturn(List.of());

        var response = templateService.copyOverrides(testApplicationId, request);

        assertThat(response.copiedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        verify(templateValuesRepository, never()).save(any());
    }

    // ========== copyOverrides Negative Tests ==========

    @Test
    void copyOverridesThrowsNotFoundForInvalidApplication() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();
        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, null, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(false);

        assertThatThrownBy(
                () -> templateService.copyOverrides(testApplicationId, request)).isInstanceOf(
                NotFoundException.class).hasMessageContaining("Application not found");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void copyOverridesThrowsNotFoundForInvalidSourceEnvironment() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();
        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, null, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(false);

        assertThatThrownBy(
                () -> templateService.copyOverrides(testApplicationId, request)).isInstanceOf(
                NotFoundException.class).hasMessageContaining("Environment not found");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void copyOverridesThrowsNotFoundForInvalidTargetEnvironment() {
        UUID sourceEnvId = UUID.randomUUID();
        UUID targetEnvId = UUID.randomUUID();
        var request = new CopyOverridesRequest(sourceEnvId, targetEnvId, null, null, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sourceEnvId)).thenReturn(true);
        when(environmentRepository.existsById(targetEnvId)).thenReturn(false);

        assertThatThrownBy(
                () -> templateService.copyOverrides(testApplicationId, request)).isInstanceOf(
                NotFoundException.class).hasMessageContaining("Environment not found");

        verify(templateValuesRepository, never()).save(any());
    }

    @Test
    void copyOverridesThrowsExceptionWhenSourceAndTargetAreSame() {
        UUID sameEnvId = UUID.randomUUID();
        var request = new CopyOverridesRequest(sameEnvId, sameEnvId, null, null, null);

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(sameEnvId)).thenReturn(true);

        assertThatThrownBy(
                () -> templateService.copyOverrides(testApplicationId, request)).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining("Source and target environments must be different");

        verify(templateValuesRepository, never()).save(any());
    }

    // ========== Helper Methods ==========

    private void setupSecurityContext(UUID companyId, UUID customerId) {
        var context = new FlagForgeSecurityContext();
        context.setCustomerName("test@example.com");
        context.setCustomerId(customerId);
        context.setCompanyId(companyId.toString());
        SecurityContextHolder.setContext(context);
    }

    private TemplateSchema createTestSchema() {
        return new TemplateSchema(
                List.of(new StringTemplateField("test_key", "Test field", true, "test_default", 0,
                        100)));
    }

    private ApplicationEntity createApplicationEntity() {
        var entity = new ApplicationEntity();
        entity.setId(testApplicationId);
        entity.setName("Test App");
        entity.setCompanyId(testCompanyId);
        return entity;
    }

    private TemplateEntity createTemplateEntity(TemplateType type) {
        var entity = new TemplateEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicationId(testApplicationId);
        entity.setCompanyId(testCompanyId);
        entity.setType(type);
        entity.setSchema(createTestSchema());
        return entity;
    }

    private TemplateEntity createTemplateEntityWithSchema(TemplateType type,
            TemplateSchema schema) {
        var entity = new TemplateEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicationId(testApplicationId);
        entity.setCompanyId(testCompanyId);
        entity.setType(type);
        entity.setSchema(schema);
        return entity;
    }

    private TemplateValuesEntity createValuesEntity(String identifier, Map<String, Object> values) {
        var entity = new TemplateValuesEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicationId(testApplicationId);
        entity.setEnvironmentId(testEnvironmentId);
        entity.setType(TemplateType.USER);
        entity.setIdentifier(identifier);
        entity.setValues(values);
        return entity;
    }

    private TemplateValuesEntity createValuesEntityForEnv(UUID environmentId, TemplateType type,
            String identifier, Map<String, Object> values) {
        var entity = new TemplateValuesEntity();
        entity.setId(UUID.randomUUID());
        entity.setApplicationId(testApplicationId);
        entity.setEnvironmentId(environmentId);
        entity.setType(type);
        entity.setIdentifier(identifier);
        entity.setValues(values);
        return entity;
    }
}
