package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-user template override values for USER templates. The user_id is a deterministic UUID derived
 * from the customer's user identifier string. Access control is enforced via application access
 * filter.
 * <p>
 * Extends {@link AuditableEntity} for automatic audit tracking.
 */
@Entity
@Table(name = "user_template_values")
@Filter(name = ApplicationOwnedEntity.APPLICATION_ACCESS_FILTER,
        condition = "application_id IN (:accessibleAppIds)")
public class UserTemplateValuesEntity extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> values;

    // Audit fields (createdAt, updatedAt, createdBy, updatedBy) inherited from AuditableEntity

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    // getCreatedAt(), getUpdatedAt(), getCreatedBy(), getUpdatedBy() inherited from AuditableEntity

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserTemplateValuesEntity that = (UserTemplateValuesEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
