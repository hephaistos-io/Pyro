package io.hephaistos.flagforge.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "company_invite")
@Filter(name = CompanyOwnedEntity.COMPANY_FILTER, condition = "company_id = :companyId")
public class CompanyInviteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String email;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "assigned_role", nullable = false)
    private CustomerRole assignedRole = CustomerRole.READ_ONLY;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by")
    private UUID usedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToMany
    @JoinTable(name = "company_invite_application_access",
            joinColumns = @JoinColumn(name = "invite_id"),
            inverseJoinColumns = @JoinColumn(name = "application_id"))
    private Set<ApplicationEntity> preAssignedApplications = new HashSet<>();

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    public Set<UUID> getPreAssignedApplicationIds() {
        return preAssignedApplications.stream()
                .map(ApplicationEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public CustomerRole getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(CustomerRole assignedRole) {
        this.assignedRole = assignedRole;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public UUID getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(UUID usedBy) {
        this.usedBy = usedBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<ApplicationEntity> getPreAssignedApplications() {
        return preAssignedApplications;
    }

    public void setPreAssignedApplications(Set<ApplicationEntity> preAssignedApplications) {
        this.preAssignedApplications = preAssignedApplications;
    }
}
