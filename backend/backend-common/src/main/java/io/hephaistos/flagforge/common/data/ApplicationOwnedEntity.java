package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Base class for entities owned by an Application. Queries are automatically filtered to only show
 * data for applications the current customer has access to (IDs cached in security context).
 * <p>
 * Entities extending this class will inherit the application_id field and the Hibernate filter for
 * automatic access control.
 * <p>
 * Extends {@link AuditableEntity} to provide automatic audit tracking (createdAt, updatedAt,
 * createdBy, updatedBy).
 */
@MappedSuperclass
@FilterDef(name = ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER,
        parameters = @ParamDef(name = "accessibleAppIds", type = UUID.class))
@Filter(name = ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER,
        condition = "application_id IN (:accessibleAppIds)")
public abstract class ApplicationOwnedEntity extends AuditableEntity {

    public static final String APPLICATION_ACCESS_FILTER = "applicationAccessFilter";

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }
}
