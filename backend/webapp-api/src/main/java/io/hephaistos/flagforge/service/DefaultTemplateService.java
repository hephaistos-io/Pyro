package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.AllTemplateOverridesResponse;
import io.hephaistos.flagforge.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.controller.dto.TemplateResponse;
import io.hephaistos.flagforge.controller.dto.TemplateUpdateRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesRequest;
import io.hephaistos.flagforge.controller.dto.TemplateValuesResponse;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.TemplateEntity;
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

        // Check if template exists
        if (!templateRepository.existsByApplicationIdAndType(applicationId, type)) {
            throw new NotFoundException(
                    "Template of type %s not found for application".formatted(type));
        }

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

    private void validateEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new NotFoundException("Environment not found: " + environmentId);
        }
    }
}
