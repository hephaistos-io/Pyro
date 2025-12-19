package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.controller.dto.ApplicationListResponse;
import io.hephaistos.flagforge.controller.dto.ApplicationResponse;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse createApplication(ApplicationCreationRequest request);

    List<ApplicationListResponse> getApplications();
}
