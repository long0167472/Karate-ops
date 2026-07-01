package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubRoster;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClubRosterRepository extends JpaRepository<ClubRoster, UUID> {
  List<ClubRoster> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<ClubRoster> findByAthlete_IdAndStatusAndDeletedAtIsNull(UUID athleteId, ClubRosterStatus status);

  Optional<ClubRoster> findByIdAndDeletedAtIsNull(UUID id);

  Optional<ClubRoster> findByOrganization_IdAndAthlete_IdAndDeletedAtIsNull(UUID organizationId, UUID athleteId);

  Optional<ClubRoster> findByOrganization_IdAndAthlete_IdAndStatusAndDeletedAtIsNull(
      UUID organizationId,
      UUID athleteId,
      ClubRosterStatus status
  );

  @Query("""
      select r.organization.id as organizationId, count(r) as total
      from ClubRoster r
      where r.organization.id in :organizationIds
        and r.status = :status
        and r.deletedAt is null
      group by r.organization.id
      """)
  List<OrganizationCountProjection> countByOrganizationIdsAndStatus(
      @Param("organizationIds") List<UUID> organizationIds,
      @Param("status") ClubRosterStatus status
  );

  interface OrganizationCountProjection {
    UUID getOrganizationId();

    Long getTotal();
  }
}
