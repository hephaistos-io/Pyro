package io.hephaistos.flagforge.customerapi.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Table(name = "application")
@Immutable
public class ApplicationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getCompanyId() {
        return companyId;
    }
}
