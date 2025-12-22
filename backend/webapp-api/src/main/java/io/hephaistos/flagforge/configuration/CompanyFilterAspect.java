package io.hephaistos.flagforge.configuration;

import io.hephaistos.flagforge.common.data.CompanyOwnedEntity;
import io.hephaistos.flagforge.security.FlagForgeSecurityContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that enables the company filter for all service layer operations when a valid company
 * context is available.
 * <p>
 * This ensures that queries on CompanyOwnedEntity subclasses are automatically filtered by the
 * current user's company.
 */
@Aspect
@Component
public class CompanyFilterAspect {

    private final EntityManager entityManager;

    public CompanyFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* io.hephaistos.flagforge.service.*.*(..))")
    public void enableCompanyFilter() {
        var context = SecurityContextHolder.getContext();

        if (context instanceof FlagForgeSecurityContext flagForgeContext) {
            flagForgeContext.getCompanyId().ifPresent(this::enableFilter);
        }
    }

    private void enableFilter(UUID companyId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(CompanyOwnedEntity.COMPANY_FILTER)
                .setParameter("companyId", companyId);
    }
}
