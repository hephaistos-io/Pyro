package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.data.TemplateValuesEntity;
import io.hephaistos.flagforge.common.data.UserTemplateValuesEntity;
import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.util.TemplateMerger;
import io.hephaistos.flagforge.common.util.UserIdHasher;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.customerapi.data.repository.TemplateRepository;
import io.hephaistos.flagforge.customerapi.data.repository.TemplateValuesRepository;
import io.hephaistos.flagforge.customerapi.data.repository.UserTemplateValuesRepository;
import io.hephaistos.flagforge.customerapi.exception.NotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DefaultTemplateService implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateValuesRepository templateValuesRepository;
    private final UserTemplateValuesRepository userTemplateValuesRepository;

    public DefaultTemplateService(TemplateRepository templateRepository,
            TemplateValuesRepository templateValuesRepository,
            UserTemplateValuesRepository userTemplateValuesRepository) {
        this.templateRepository = templateRepository;
        this.templateValuesRepository = templateValuesRepository;
        this.userTemplateValuesRepository = userTemplateValuesRepository;
    }

    @Override
    public MergedTemplateValuesResponse getMergedSystemValues(UUID applicationId,
            UUID environmentId, @Nullable String identifier) {

        // Get template schema for default values
        var template =
                templateRepository.findByApplicationIdAndType(applicationId, TemplateType.SYSTEM)
                        .orElseThrow(() -> new NotFoundException(
                                "SYSTEM template not found for application: " + applicationId));

        // Get override values if identifier provided
        Map<String, Object> overrideValues = null;
        String appliedIdentifier = null;

        if (identifier != null && !identifier.isBlank()) {
            var override =
                    templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                            applicationId, environmentId, TemplateType.SYSTEM, identifier);
            if (override.isPresent()) {
                overrideValues = override.get().getValues();
                appliedIdentifier = identifier;
            }
        }

        // Merge defaults with override using shared utility
        var mergedValues = TemplateMerger.merge(template.getSchema(), overrideValues);

        return new MergedTemplateValuesResponse(TemplateType.SYSTEM, template.getSchema(),
                mergedValues, appliedIdentifier);
    }

    @Override
    public MergedTemplateValuesResponse getMergedUserValues(UUID applicationId, UUID environmentId,
            String userId) {

        // 1. Get USER template schema (defaults)
        var template =
                templateRepository.findByApplicationIdAndType(applicationId, TemplateType.USER)
                        .orElseThrow(() -> new NotFoundException(
                                "USER template not found for application: " + applicationId));

        // 2. Get environment-level defaults (identifier = "")
        Map<String, Object> environmentDefaults =
                templateValuesRepository.findByApplicationIdAndEnvironmentIdAndTypeAndIdentifier(
                                applicationId, environmentId, TemplateType.USER, "")
                        .map(TemplateValuesEntity::getValues)
                        .orElse(null);

        // 3. Get user-specific overrides
        UUID userUuid = UserIdHasher.toUuid(userId);
        Map<String, Object> userOverrides =
                userTemplateValuesRepository.findByApplicationIdAndEnvironmentIdAndUserId(
                                applicationId, environmentId, userUuid)
                        .map(UserTemplateValuesEntity::getValues)
                        .orElse(null);

        // 4. Merge: schema defaults → environment defaults → user overrides
        var merged = TemplateMerger.merge(template.getSchema(), environmentDefaults);
        if (userOverrides != null) {
            merged.putAll(userOverrides);
        }

        return new MergedTemplateValuesResponse(TemplateType.USER, template.getSchema(), merged,
                userId);
    }

    @Override
    @Transactional
    public void setUserValues(UUID applicationId, UUID environmentId, String userId,
            Map<String, Object> values) {

        UUID userUuid = UserIdHasher.toUuid(userId);

        var existing = userTemplateValuesRepository.findByApplicationIdAndEnvironmentIdAndUserId(
                applicationId, environmentId, userUuid);

        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setValues(values);
            // updatedAt handled by @PreUpdate
        }
        else {
            var entity = new UserTemplateValuesEntity();
            entity.setApplicationId(applicationId);
            entity.setEnvironmentId(environmentId);
            entity.setUserId(userUuid);
            entity.setValues(values);
            userTemplateValuesRepository.save(entity);
        }
    }
}
