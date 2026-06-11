package com.karate.tournament.repository;

import com.karate.tournament.entity.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
  List<Organization> findByDeletedAtIsNullOrderByNameAsc();

  Optional<Organization> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Organization> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);
}
