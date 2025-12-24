package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.common.data.ApplicationEntity;
import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;
import io.hephaistos.flagforge.controller.dto.EnvironmentUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface EnvironmentService {

    EnvironmentResponse createEnvironment(UUID applicationId, EnvironmentCreationRequest request);

    EnvironmentResponse createDefaultEnvironment(UUID applicationId);

    void createDefaultEnvironments(ApplicationEntity application);

    List<EnvironmentResponse> getEnvironmentsForApplication(UUID applicationId);

    EnvironmentResponse updateEnvironment(UUID applicationId, UUID environmentId,
            EnvironmentUpdateRequest request);

    void deleteEnvironment(UUID applicationId, UUID environmentId);
}
