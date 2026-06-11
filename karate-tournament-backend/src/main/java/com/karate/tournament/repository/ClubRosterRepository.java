package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubRoster;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRosterRepository extends JpaRepository<ClubRoster, UUID> {
  List<ClubRoster> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  Optional<ClubRoster> findByIdAndDeletedAtIsNull(UUID id);

  Optional<ClubRoster> findByOrganization_IdAndAthlete_IdAndDeletedAtIsNull(UUID organizationId, UUID athleteId);

  Optional<ClubRoster> findByOrganization_IdAndAthlete_IdAndStatusAndDeletedAtIsNull(
      UUID organizationId,
      UUID athleteId,
      ClubRosterStatus status
  );
}
