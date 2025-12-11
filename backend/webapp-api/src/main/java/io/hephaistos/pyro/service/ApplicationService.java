package io.hephaistos.pyro.service;

import io.hephaistos.pyro.controller.dto.ApplicationCreationRequest;
import io.hephaistos.pyro.controller.dto.ApplicationResponse;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse createApplication(ApplicationCreationRequest request);

    List<ApplicationResponse> getApplicationsForCurrentUserCompany();
}
