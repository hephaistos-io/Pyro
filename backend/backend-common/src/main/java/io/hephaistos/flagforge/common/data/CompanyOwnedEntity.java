package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Base class for entities that are owned by a company. The companyId field is filtered by
 * Hibernate's @Filter mechanism to ensure data isolation between companies.
 * <p>
 * Unlike @TenantId, this approach allows: - Operations without a tenant context during
 * bootstrap/tests - Explicit control over when filtering is enabled - Admin operations that need to
 * access all data
 * <p>
 * Extends {@link AuditableEntity} to provide automatic audit tracking (createdAt, updatedAt,
 * createdBy, updatedBy).
 */
@MappedSuperclass
@FilterDef(name = CompanyOwnedEntity.COMPANY_FILTER,
        parameters = @ParamDef(name = "companyId", type = UUID.class))
@Filter(name = CompanyOwnedEntity.COMPANY_FILTER, condition = "company_id = :companyId")
public abstract class CompanyOwnedEntity extends AuditableEntity {

    public static final String COMPANY_FILTER = "companyFilter";

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
}
