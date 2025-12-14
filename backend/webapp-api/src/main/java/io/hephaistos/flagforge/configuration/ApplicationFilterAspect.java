package io.hephaistos.flagforge.configuration;

import io.hephaistos.flagforge.data.ApplicationOwnedEntity;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Aspect that enables the application access filter for all service layer operations. Uses cached
 * accessible application IDs from the security context.
 * <p>
 * This ensures that queries on entities with the applicationAccessFilter are automatically filtered
 * to only return data for applications the current customer has access to.
 */
@Aspect
@Component
public class ApplicationFilterAspect {

    private final EntityManager entityManager;

    public ApplicationFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* io.hephaistos.flagforge.service.*.*(..))")
    public void enableApplicationAccessFilter() {
        var context = SecurityContextHolder.getContext();

        if (context instanceof FlagForgeSecurityContext flagForgeContext) {
            enableFilter(flagForgeContext.getAccessibleApplicationIds());
        }
    }

    private void enableFilter(Set<UUID> accessibleAppIds) {
        Session session = entityManager.unwrap(Session.class);

        // Handle empty set - use a non-matching UUID to return no results
        Set<UUID> filterValue =
                accessibleAppIds.isEmpty() ? Set.of(new UUID(0, 0)) : accessibleAppIds;

        session.enableFilter(ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER)
                .setParameterList("accessibleAppIds", filterValue);
    }
}
