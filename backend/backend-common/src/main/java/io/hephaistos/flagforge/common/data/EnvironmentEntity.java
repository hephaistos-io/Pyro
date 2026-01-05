package io.hephaistos.flagforge.common.data;

import io.hephaistos.flagforge.common.enums.PaymentStatus;
import io.hephaistos.flagforge.common.enums.PricingTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "environment")
public class EnvironmentEntity extends ApplicationOwnedEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private ApplicationEntity application;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PricingTier tier = PricingTier.BASIC;

    @Column(name = "rate_limit_requests_per_second", nullable = false)
    private Integer rateLimitRequestsPerSecond = 5;

    @Column(name = "requests_per_month", nullable = false)
    private Integer requestsPerMonth = 500000;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ApplicationEntity getApplication() {
        return application;
    }

    public void setApplication(ApplicationEntity application) {
        this.application = application;
        if (application != null) {
            setApplicationId(application.getId());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PricingTier getTier() {
        return tier;
    }

    public void setTier(PricingTier tier) {
        this.tier = tier;
    }

    public Integer getRateLimitRequestsPerSecond() {
        return rateLimitRequestsPerSecond;
    }

    public void setRateLimitRequestsPerSecond(Integer rateLimitRequestsPerSecond) {
        this.rateLimitRequestsPerSecond = rateLimitRequestsPerSecond;
    }

    public Integer getRequestsPerMonth() {
        return requestsPerMonth;
    }

    public void setRequestsPerMonth(Integer requestsPerMonth) {
        this.requestsPerMonth = requestsPerMonth;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
