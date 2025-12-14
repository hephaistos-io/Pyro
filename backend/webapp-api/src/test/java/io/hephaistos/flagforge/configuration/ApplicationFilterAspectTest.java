package io.hephaistos.flagforge.configuration;

import io.hephaistos.flagforge.data.ApplicationOwnedEntity;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ApplicationFilterAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @Mock
    private SecurityContext genericSecurityContext;

    private ApplicationFilterAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new ApplicationFilterAspect(entityManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enablesFilterWithAccessibleApplicationIds() {
        // Given
        UUID appId1 = UUID.randomUUID();
        UUID appId2 = UUID.randomUUID();
        Set<UUID> accessibleAppIds = Set.of(appId1, appId2);

        FlagForgeSecurityContext securityContext = new FlagForgeSecurityContext();
        securityContext.setAccessibleApplicationIds(accessibleAppIds);
        SecurityContextHolder.setContext(securityContext);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER)).thenReturn(
                filter);
        when(filter.setParameterList(eq("accessibleAppIds"), any(Collection.class))).thenReturn(
                filter);

        // When
        aspect.enableApplicationAccessFilter();

        // Then
        verify(session).enableFilter(ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(filter).setParameterList(eq("accessibleAppIds"), captor.capture());

        assertThat(captor.getValue()).containsExactlyInAnyOrderElementsOf(accessibleAppIds);
    }

    @Test
    void usesNonMatchingUuidWhenAccessibleApplicationIdsIsEmpty() {
        // Given
        FlagForgeSecurityContext securityContext = new FlagForgeSecurityContext();
        securityContext.setAccessibleApplicationIds(Set.of());
        SecurityContextHolder.setContext(securityContext);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER)).thenReturn(
                filter);
        when(filter.setParameterList(eq("accessibleAppIds"), any(Collection.class))).thenReturn(
                filter);

        // When
        aspect.enableApplicationAccessFilter();

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(filter).setParameterList(eq("accessibleAppIds"), captor.capture());

        // Should be an empty set
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue()).containsExactly(new UUID(0, 0));
    }

    @Test
    void doesNotEnableFilterWhenSecurityContextIsNotFlagForgeSecurityContext() {
        // Given - using a generic SecurityContext that is not FlagForgeSecurityContext
        SecurityContextHolder.setContext(genericSecurityContext);

        // When
        aspect.enableApplicationAccessFilter();

        // Then - filter should not be enabled
        verify(entityManager, never()).unwrap(Session.class);
    }
}
