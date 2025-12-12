package io.hephaistos.flagforge.service;

import io.hephaistos.flagforge.controller.dto.ApplicationCreationRequest;
import io.hephaistos.flagforge.data.ApplicationEntity;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import io.hephaistos.flagforge.exception.DuplicateResourceException;
import io.hephaistos.flagforge.exception.NoCompanyAssignedException;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    private DefaultApplicationService applicationService;
    private UUID testCompanyId;

    @BeforeEach
    void setUp() {
        applicationService = new DefaultApplicationService(applicationRepository);
        testCompanyId = UUID.randomUUID();
        setupSecurityContext(testCompanyId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplicationSucceeds() {
        var request = new ApplicationCreationRequest("Test App");
        when(applicationRepository.existsByNameAndCompanyId("Test App", testCompanyId)).thenReturn(
                false);
        when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(invocation -> {
            ApplicationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        var response = applicationService.createApplication(request);

        assertThat(response.name()).isEqualTo("Test App");
        assertThat(response.companyId()).isEqualTo(testCompanyId);
    }

    @Test
    void createApplicationThrowsDuplicateResourceException() {
        var request = new ApplicationCreationRequest("Existing App");
        when(applicationRepository.existsByNameAndCompanyId("Existing App",
                testCompanyId)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(request)).isInstanceOf(
                DuplicateResourceException.class).hasMessageContaining("Existing App");
    }

    @Test
    void createApplicationThrowsNoCompanyAssignedException() {
        setupSecurityContextWithoutCompany();
        var request = new ApplicationCreationRequest("Test App");

        assertThatThrownBy(() -> applicationService.createApplication(request)).isInstanceOf(
                NoCompanyAssignedException.class);
    }

    @Test
    void getApplicationsReturnsListForCompany() {
        var app1 = createApplicationEntity("App 1", testCompanyId);
        var app2 = createApplicationEntity("App 2", testCompanyId);
        when(applicationRepository.findByCompanyId(testCompanyId)).thenReturn(List.of(app1, app2));

        var result = applicationService.getApplicationsForCurrentUserCompany();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("name").containsExactly("App 1", "App 2");
    }

    @Test
    void getApplicationsReturnsEmptyListWhenNone() {
        when(applicationRepository.findByCompanyId(testCompanyId)).thenReturn(List.of());

        var result = applicationService.getApplicationsForCurrentUserCompany();

        assertThat(result).isEmpty();
    }

    @Test
    void getApplicationsThrowsNoCompanyAssignedException() {
        setupSecurityContextWithoutCompany();

        assertThatThrownBy(
                () -> applicationService.getApplicationsForCurrentUserCompany()).isInstanceOf(
                NoCompanyAssignedException.class);
    }

    private void setupSecurityContext(UUID companyId) {
        var context = new FlagForgeSecurityContext();
        context.setUserName("test@example.com");
        context.setUserId(UUID.randomUUID().toString());
        context.setCompanyId(companyId.toString());
        SecurityContextHolder.setContext(context);
    }

    private void setupSecurityContextWithoutCompany() {
        var context = new FlagForgeSecurityContext();
        context.setUserName("test@example.com");
        context.setUserId(UUID.randomUUID().toString());
        SecurityContextHolder.setContext(context);
    }

    private ApplicationEntity createApplicationEntity(String name, UUID companyId) {
        var entity = new ApplicationEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setCompanyId(companyId);
        return entity;
    }
}
