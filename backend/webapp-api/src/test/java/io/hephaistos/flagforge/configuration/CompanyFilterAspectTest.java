package io.hephaistos.flagforge.configuration;

import io.hephaistos.flagforge.common.data.CompanyOwnedEntity;
import io.hephaistos.flagforge.common.security.FlagForgeSecurityContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CompanyFilterAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @Mock
    private SecurityContext genericSecurityContext;

    private CompanyFilterAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CompanyFilterAspect(entityManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enablesFilterWithCompanyId() {
        // Given
        UUID companyId = UUID.randomUUID();

        FlagForgeSecurityContext securityContext = new FlagForgeSecurityContext();
        securityContext.setCompanyId(companyId);
        SecurityContextHolder.setContext(securityContext);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(CompanyOwnedEntity.COMPANY_FILTER)).thenReturn(filter);
        when(filter.setParameter("companyId", companyId)).thenReturn(filter);

        // When
        aspect.enableCompanyFilter();

        // Then
        verify(session).enableFilter(CompanyOwnedEntity.COMPANY_FILTER);
        verify(filter).setParameter("companyId", companyId);
    }

    @Test
    void doesNotEnableFilterWhenCompanyIdIsNotPresent() {
        // Given - context without companyId
        FlagForgeSecurityContext securityContext = new FlagForgeSecurityContext();
        SecurityContextHolder.setContext(securityContext);

        // When
        aspect.enableCompanyFilter();

        // Then - filter should not be enabled
        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void doesNotEnableFilterWhenSecurityContextIsNotFlagForgeSecurityContext() {
        // Given - using a generic SecurityContext that is not FlagForgeSecurityContext
        SecurityContextHolder.setContext(genericSecurityContext);

        // When
        aspect.enableCompanyFilter();

        // Then - filter should not be enabled
        verify(entityManager, never()).unwrap(Session.class);
    }
}
