package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationListResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationStatisticsResponse;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse createApplication(ApplicationCreationRequest request);

    List<ApplicationListResponse> getApplications();

    ApplicationStatisticsResponse getApplicationStatistics(UUID applicationId);
}
