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
import io.hephaistos.flagforge.data.TemplateType;

import java.util.List;
import java.util.UUID;

public interface TemplateService {

    // Template initialization (called when application is created)
    void createDefaultTemplates(ApplicationEntity application);

    // Template CRUD (customers can only list, view, and update - not create or delete)
    List<TemplateResponse> getTemplates(UUID applicationId);

    TemplateResponse getTemplateByType(UUID applicationId, TemplateType type);

    TemplateResponse updateTemplate(UUID applicationId, TemplateType type,
            TemplateUpdateRequest request);

    // Template Values (overrides)
    MergedTemplateValuesResponse getMergedValues(UUID applicationId, UUID environmentId,
            TemplateType type, List<String> identifiers);

    AllTemplateOverridesResponse listAllOverrides(UUID applicationId, UUID environmentId);

    List<TemplateValuesResponse> listOverrides(UUID applicationId, UUID environmentId,
            TemplateType type);

    TemplateValuesResponse setOverride(UUID applicationId, UUID environmentId, TemplateType type,
            String identifier, TemplateValuesRequest request);

    void deleteOverride(UUID applicationId, UUID environmentId, TemplateType type,
            String identifier);

    CopyOverridesResponse copyOverrides(UUID applicationId, CopyOverridesRequest request);
}
