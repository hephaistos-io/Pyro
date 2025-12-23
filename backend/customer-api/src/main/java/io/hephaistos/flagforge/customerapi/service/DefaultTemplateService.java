package io.hephaistos.flagforge.customerapi.service;

import io.hephaistos.flagforge.common.enums.TemplateType;
import io.hephaistos.flagforge.common.util.TemplateMerger;
import io.hephaistos.flagforge.customerapi.controller.dto.MergedTemplateValuesResponse;
import io.hephaistos.flagforge.customerapi.data.repository.TemplateRepository;
import io.hephaistos.flagforge.customerapi.data.repository.TemplateValuesRepository;
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

    public DefaultTemplateService(TemplateRepository templateRepository,
            TemplateValuesRepository templateValuesRepository) {
        this.templateRepository = templateRepository;
        this.templateValuesRepository = templateValuesRepository;
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
}
