package io.hephaistos.flagforge.customerapi.data.repository;

import io.hephaistos.flagforge.customerapi.data.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
}
