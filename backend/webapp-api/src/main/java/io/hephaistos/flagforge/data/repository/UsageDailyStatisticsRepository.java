package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.UsageDailyStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageDailyStatisticsRepository
        extends JpaRepository<UsageDailyStatisticsEntity, UUID> {

    List<UsageDailyStatisticsEntity> findByEnvironmentIdAndDateGreaterThanEqualOrderByDateDesc(
            UUID environmentId, LocalDate startDate);

    Optional<UsageDailyStatisticsEntity> findByEnvironmentIdAndDate(UUID environmentId,
            LocalDate date);
}
