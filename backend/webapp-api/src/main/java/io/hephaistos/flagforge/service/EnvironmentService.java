package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.EnvironmentCreationRequest;
import io.hephaistos.flagforge.controller.dto.EnvironmentResponse;

import java.util.List;
import java.util.UUID;

public interface EnvironmentService {

    EnvironmentResponse createEnvironment(UUID applicationId, EnvironmentCreationRequest request);

    EnvironmentResponse createDefaultEnvironment(UUID applicationId);

    List<EnvironmentResponse> getEnvironmentsForApplication(UUID applicationId);

    void deleteEnvironment(UUID applicationId, UUID environmentId);
}
