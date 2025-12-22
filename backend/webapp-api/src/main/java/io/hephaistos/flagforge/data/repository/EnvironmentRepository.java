package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.common.data.EnvironmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {

    List<EnvironmentEntity> findByApplication_Id(UUID applicationId);

    boolean existsByNameAndApplication_Id(String name, UUID applicationId);
}
