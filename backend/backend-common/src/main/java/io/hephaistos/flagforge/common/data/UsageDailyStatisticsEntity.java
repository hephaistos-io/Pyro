package io.hephaistos.flagforge.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usage_daily_statistics")
public class UsageDailyStatisticsEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "total_requests", nullable = false)
    private long totalRequests;

    @Column(name = "peak_requests_per_second", nullable = false)
    private int peakRequestsPerSecond;

    @Column(name = "avg_requests_per_second", nullable = false)
    private BigDecimal avgRequestsPerSecond = BigDecimal.ZERO;

    @Column(name = "rejected_requests", nullable = false)
    private long rejectedRequests = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(UUID environmentId) {
        this.environmentId = environmentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getPeakRequestsPerSecond() {
        return peakRequestsPerSecond;
    }

    public void setPeakRequestsPerSecond(int peakRequestsPerSecond) {
        this.peakRequestsPerSecond = peakRequestsPerSecond;
    }

    public BigDecimal getAvgRequestsPerSecond() {
        return avgRequestsPerSecond;
    }

    public void setAvgRequestsPerSecond(BigDecimal avgRequestsPerSecond) {
        this.avgRequestsPerSecond = avgRequestsPerSecond;
    }

    public long getRejectedRequests() {
        return rejectedRequests;
    }

    public void setRejectedRequests(long rejectedRequests) {
        this.rejectedRequests = rejectedRequests;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
