package io.hephaistos.flagforge.data.repository;

import io.hephaistos.flagforge.data.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {

    List<ApplicationEntity> findByCompanyId(UUID companyId);

    boolean existsByNameAndCompanyId(String name, UUID companyId);

    long countByCompanyId(UUID companyId);
}
