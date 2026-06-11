package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubFeeRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubFeeRoleRepository extends JpaRepository<ClubFeeRole, UUID> {
  List<ClubFeeRole> findByOrganization_IdAndDeletedAtIsNullOrderByPriorityAscNameAsc(UUID organizationId);

  Optional<ClubFeeRole> findByIdAndDeletedAtIsNull(UUID id);
}
