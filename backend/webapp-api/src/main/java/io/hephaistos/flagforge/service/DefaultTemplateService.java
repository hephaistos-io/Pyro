package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.AllTemplateOverridesResponse;
import io.hephaistos.flagforge.controller.dto.CopyOverridesRequest;
import io.hephaistos.flagforge.controller.dto.CopyOverridesResponse;
import io.hephaistos.flagforge.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.controller.dto.TemplateResponse;
import io.hephaistos.flagforge.controller.dto.TemplateUpdateRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesResponse;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.BooleanTemplateField;
import io.hephaistos.flagforge.data.EnumTemplateField;
import io.hephaistos.flagforge.data.NumberTemplateField;
import io.hephaistos.flagforge.data.StringTemplateField;
import io.hephaistos.flagforge.data.TemplateEntity;
import io.hephaistos.flagforge.data.TemplateField;
import io.hephaistos.flagforge.data.TemplateSchema;
import io.hephaistos.flagforge.data.TemplateType;
import io.hephaistos.flagforge.data.TemplateValuesEntity;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.data.repository.EnvironmentRepository;
import io.hephaistos.flagforge.data.repository.TemplateRepository;
import io.hephaistos.flagforge.data.repository.TemplateValuesRepository;
import io.hephaistos.flagforge.exception.NotFoundException;
import io.hephaistos.flagforge.security.RequireAdmin;
import io.hephaistos.flagforge.security.RequireDev;
import io.hephaistos.flagforge.security.RequireReadOnly;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DefaultTemplateService implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateValuesRepository templateValuesRepository;
    private final ApplicationRepository applicationRepository;
    private final EnvironmentRepository environmentRepository;

    public DefaultTemplateService(TemplateRepository templateRepository,
            TemplateValuesRepository templateValuesRepository,
            ApplicationRepository applicationRepository,
            EnvironmentRepository environmentRepository) {
        this.templateRepository = templateRepository;
        this.templateValuesRepository = templateValuesRepository;
        this.applicationRepository = applicationRepository;
        this.environmentRepository = environmentRepository;
    }

    @Override
    public void createDefaultTemplates(ApplicationEntity application) {
        var emptySchema = new TemplateSchema(Collections.emptyList());

        for (TemplateType type : TemplateType.values()) {
            var template = new TemplateEntity();
            template.setCompanyId(application.getCompanyId());
            template.setType(type);
            template.setSchema(emptySchema);
            application.addTemplate(template);
        }
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplates(UUID applicationId) {
        // Verify application access
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        return templateRepository.findByApplicationId(applicationId)
                .stream()
                .map(TemplateResponse::fromEntity)
                .toList();
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateByType(UUID applicationId, TemplateType type) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        var template = templateRepository.findByApplicationIdAndType(applicationId, type)
                .orElseThrow(() -> new NotFoundException(
                        "Template of type %s not found for application".formatted(type)));
        return TemplateResponse.fromEntity(template);
    }

    @Override
    @RequireAdmin
    public TemplateResponse updateTemplate(UUID applicationId, TemplateType type,
            TemplateUpdateRequest request) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        var template = templateRepository.findByApplicationIdAndType(applicationId, type)
                .orElseThrow(() -> new NotFoundException(
                        "Template of type %s not found for application".formatted(type)));

        template.setSchema(request.schema());
        templateRepository.save(template);
        return TemplateResponse.fromEntity(template);
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public MergedTemplateValuesResponse getMergedValues(UUID applicationId, UUID environmentId,
            TemplateType type, List<String> identifiers) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        validateEnvironment(environmentId);

        // Get template schema for default values
        var template = templateRepository.findByApplicationIdAndType(applicationId, type)
                .orElseThrow(() -> new NotFoundException(
                        "Template of type %s not found for application".formatted(type)));

        // Start with defaults from schema
        Map<String, Object> mergedValues =
                new LinkedHashMap<>(template.getSchema().getDefaultValues());

        // Apply overrides in order if identifiers provided
        List<String> appliedIdentifiers = new ArrayList<>();
        if (identifiers != null && !identifiers.isEmpty()) {
            // Fetch all matching overrides
            var overrides =
                    templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifierIn(
                            applicationId, environmentId, type, identifiers);

            // Create a map for O(1) lookup
            Map<String, TemplateValuesEntity> overridesByIdentifier = new HashMap<>();
            for (var override : overrides) {
                overridesByIdentifier.put(override.getIdentifier(), override);
            }

            // Apply in the order specified by identifiers
            for (String identifier : identifiers) {
                var override = overridesByIdentifier.get(identifier);
                if (override != null) {
                    mergedValues.putAll(override.getValues());
                    appliedIdentifiers.add(identifier);
                }
            }
        }

        return new MergedTemplateValuesResponse(applicationId, environmentId, type,
                template.getSchema(), mergedValues, appliedIdentifiers);
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public AllTemplateOverridesResponse listAllOverrides(UUID applicationId, UUID environmentId) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        validateEnvironment(environmentId);

        var allOverrides =
                templateValuesRepository.findByApplicationIdAndEnvironmentId(applicationId,
                        environmentId);

        var userOverrides = allOverrides.stream()
                .filter(o -> o.getType() == TemplateType.USER)
                .map(TemplateValuesResponse::fromEntity)
                .toList();

        var systemOverrides = allOverrides.stream()
                .filter(o -> o.getType() == TemplateType.SYSTEM)
                .map(TemplateValuesResponse::fromEntity)
                .toList();

        return new AllTemplateOverridesResponse(userOverrides, systemOverrides);
    }

    @Override
    @RequireReadOnly
    @Transactional(readOnly = true)
    public List<TemplateValuesResponse> listOverrides(UUID applicationId, UUID environmentId,
            TemplateType type) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        validateEnvironment(environmentId);

        return templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(applicationId,
                environmentId, type).stream().map(TemplateValuesResponse::fromEntity).toList();
    }

    @Override
    @RequireDev
    public TemplateValuesResponse setOverride(UUID applicationId, UUID environmentId,
            TemplateType type, String identifier, TemplateValuesRequest request) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        validateEnvironment(environmentId);

        // Fetch template to validate against schema
        var template = templateRepository.findByApplicationIdAndType(applicationId, type)
                .orElseThrow(() -> new NotFoundException(
                        "Template of type %s not found for application".formatted(type)));

        // Validate override values against template schema
        validateOverrideValues(request.values(), template.getSchema());

        // Find existing or create new
        var existingOverride =
                templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                        applicationId, environmentId, type, identifier);

        TemplateValuesEntity override;
        if (existingOverride.isPresent()) {
            override = existingOverride.get();
            override.setValues(request.values());
        }
        else {
            override = new TemplateValuesEntity();
            override.setApplicationId(applicationId);
            override.setEnvironmentId(environmentId);
            override.setType(type);
            override.setIdentifier(identifier);
            override.setValues(request.values());
        }

        templateValuesRepository.save(override);
        return TemplateValuesResponse.fromEntity(override);
    }

    @Override
    @RequireDev
    public void deleteOverride(UUID applicationId, UUID environmentId, TemplateType type,
            String identifier) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        validateEnvironment(environmentId);

        var override =
                templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                                applicationId, environmentId, type, identifier)
                        .orElseThrow(() -> new NotFoundException(
                                "Override not found for identifier: " + identifier));

        templateValuesRepository.delete(override);
    }

    @Override
    @RequireDev
    public CopyOverridesResponse copyOverrides(UUID applicationId, CopyOverridesRequest request) {
        if (!applicationRepository.existsByIdFiltered(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
        validateEnvironment(request.sourceEnvironmentId());
        validateEnvironment(request.targetEnvironmentId());

        if (request.sourceEnvironmentId().equals(request.targetEnvironmentId())) {
            throw new IllegalArgumentException("Source and target environments must be different");
        }

        // Determine which types to copy
        List<TemplateType> typesToCopy = request.types() != null && !request.types().isEmpty() ?
                request.types() :
                List.of(TemplateType.values());

        boolean overwrite = request.overwrite() != null && request.overwrite();

        // Determine identifier filtering
        List<String> identifiersFilter = request.identifiers();
        boolean filterByIdentifiers = identifiersFilter != null && !identifiersFilter.isEmpty();

        int copiedCount = 0;
        int skippedCount = 0;

        for (TemplateType type : typesToCopy) {
            // Get all overrides from source environment for this type
            var sourceOverrides =
                    templateValuesRepository.findByApplicationIdAndEnvironmentIdAndType(
                            applicationId, request.sourceEnvironmentId(), type);

            for (var sourceOverride : sourceOverrides) {
                String identifier = sourceOverride.getIdentifier();

                // Skip if not in filter list
                if (filterByIdentifiers && !identifiersFilter.contains(identifier)) {
                    continue;
                }

                // Check if override already exists in target
                var existingTarget =
                        templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                                applicationId, request.targetEnvironmentId(), type, identifier);

                if (existingTarget.isPresent() && !overwrite) {
                    skippedCount++;
                    continue;
                }

                // Copy the override
                TemplateValuesEntity targetOverride;
                if (existingTarget.isPresent()) {
                    targetOverride = existingTarget.get();
                    targetOverride.setValues(new HashMap<>(sourceOverride.getValues()));
                }
                else {
                    targetOverride = new TemplateValuesEntity();
                    targetOverride.setApplicationId(applicationId);
                    targetOverride.setEnvironmentId(request.targetEnvironmentId());
                    targetOverride.setType(type);
                    targetOverride.setIdentifier(identifier);
                    targetOverride.setValues(new HashMap<>(sourceOverride.getValues()));
                }

                templateValuesRepository.save(targetOverride);
                copiedCount++;
            }
        }

        return new CopyOverridesResponse(copiedCount, skippedCount);
    }

    /**
     * Validates override values against the template schema constraints. Each value must match the
     * type and constraints of its corresponding field. Unknown fields (not in schema) are allowed
     * for flexibility.
     *
     * @param values Override values to validate
     * @param schema Template schema containing field definitions
     * @throws IllegalArgumentException if any value violates its field constraints
     */
    private void validateOverrideValues(Map<String, Object> values, TemplateSchema schema) {
        // Create a map of field key -> field for quick lookup
        Map<String, TemplateField> fieldMap = new HashMap<>();
        for (var field : schema.fields()) {
            fieldMap.put(field.key(), field);
        }

        // Validate each value against its field definition
        for (var entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Find the field definition
            TemplateField field = fieldMap.get(key);
            if (field == null) {
                // Allow fields not in schema for flexibility (forward compatibility)
                continue;
            }

            // Validate based on field type using pattern matching
            switch (field) {
                case StringTemplateField stringField ->
                        validateStringValue(key, value, stringField);
                case NumberTemplateField numberField ->
                        validateNumberValue(key, value, numberField);
                case BooleanTemplateField booleanField ->
                        validateBooleanValue(key, value, booleanField);
                case EnumTemplateField enumField -> validateEnumValue(key, value, enumField);
            }
        }
    }

    private void validateStringValue(String key, Object value, StringTemplateField field) {
        if (!(value instanceof String strValue)) {
            throw new IllegalArgumentException(
                    "Field '%s' expects a String value, but got %s".formatted(key,
                            value.getClass().getSimpleName()));
        }

        int length = strValue.length();

        if (field.minLength() != null && length < field.minLength()) {
            throw new IllegalArgumentException(
                    "Field '%s' value length (%d) is below minLength (%d)".formatted(key, length,
                            field.minLength()));
        }

        if (field.maxLength() != null && length > field.maxLength()) {
            throw new IllegalArgumentException(
                    "Field '%s' value length (%d) exceeds maxLength (%d)".formatted(key, length,
                            field.maxLength()));
        }
    }

    private void validateNumberValue(String key, Object value, NumberTemplateField field) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Field '%s' expects a Number value, but got %s".formatted(key,
                            value.getClass().getSimpleName()));
        }

        double numValue = ((Number) value).doubleValue();

        if (field.minValue() != null && numValue < field.minValue()) {
            throw new IllegalArgumentException(
                    "Field '%s' value (%.2f) is below minValue (%.2f)".formatted(key, numValue,
                            field.minValue()));
        }

        if (field.maxValue() != null && numValue > field.maxValue()) {
            throw new IllegalArgumentException(
                    "Field '%s' value (%.2f) exceeds maxValue (%.2f)".formatted(key, numValue,
                            field.maxValue()));
        }

        // Validate increment alignment
        if (field.incrementAmount() != null && field.minValue() != null) {
            double diff = numValue - field.minValue();
            double remainder = Math.abs(diff % field.incrementAmount());
            double tolerance = field.incrementAmount() * 1e-9;

            if (remainder > tolerance && remainder < field.incrementAmount() - tolerance) {
                throw new IllegalArgumentException(
                        "Field '%s' value (%.2f) must align with incrementAmount (%.2f) starting from minValue (%.2f)".formatted(
                                key, numValue, field.incrementAmount(), field.minValue()));
            }
        }
    }

    private void validateBooleanValue(String key, Object value, BooleanTemplateField field) {
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "Field '%s' expects a boolean value, but got %s".formatted(key,
                            value.getClass().getSimpleName()));
        }
    }

    private void validateEnumValue(String key, Object value, EnumTemplateField field) {
        if (!(value instanceof String strValue)) {
            throw new IllegalArgumentException(
                    "Field '%s' expects a String value (from enum options), but got %s".formatted(
                            key, value.getClass().getSimpleName()));
        }

        if (!field.options().contains(strValue)) {
            throw new IllegalArgumentException(
                    "Field '%s' value '%s' is not in the allowed options: %s".formatted(key,
                            strValue, field.options()));
        }
    }

    private void validateEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new NotFoundException("Environment not found: " + environmentId);
        }
    }
}
