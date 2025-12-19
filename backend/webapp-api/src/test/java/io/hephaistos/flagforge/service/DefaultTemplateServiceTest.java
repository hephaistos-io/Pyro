package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.TemplateUpdateRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesRequest;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.StringTemplateField;
import io.hephaistos.flagforge.data.TemplateEntity;
import io.hephaistos.flagforge.data.TemplateSchema;
import io.hephaistos.flagforge.data.TemplateType;
import io.hephaistos.flagforge.data.TemplateValuesEntity;
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
import static org.mockito.Mockito.never;
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

    private DefaultTemplateService templateService;
    private UUID testCompanyId;
    private UUID testApplicationId;
    private UUID testEnvironmentId;

    @BeforeEach
    void setUp() {
        templateService = new DefaultTemplateService(templateRepository, templateValuesRepository,
                applicationRepository, environmentRepository);
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
        var request = new TemplateValuesRequest(Map.of("test_key", "override_value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.existsByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(true);
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
        var existingOverride = createValuesEntity("user-123", Map.of("old_key", "old_value"));
        var request = new TemplateValuesRequest(Map.of("new_key", "new_value"));

        when(applicationRepository.existsByIdFiltered(testApplicationId)).thenReturn(true);
        when(environmentRepository.existsById(testEnvironmentId)).thenReturn(true);
        when(templateRepository.existsByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(true);
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
        when(templateRepository.existsByApplicationIdAndType(testApplicationId,
                TemplateType.USER)).thenReturn(false);

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
}
