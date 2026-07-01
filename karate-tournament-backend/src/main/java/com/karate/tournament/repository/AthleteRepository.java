package com.karate.tournament.repository;

import com.karate.tournament.entity.Athlete;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AthleteRepository extends JpaRepository<Athlete, UUID> {
  List<Athlete> findByDeletedAtIsNullOrderByCreatedAtDesc();

  @Query("""
      select distinct athlete
      from Athlete athlete
      left join ClubRoster roster
        on roster.athlete = athlete
       and roster.deletedAt is null
       and roster.status = com.karate.tournament.entity.enums.ClubRosterStatus.ACTIVE
      where athlete.deletedAt is null
        and (
          athlete.primaryOrganization.id = :organizationId
          or roster.organization.id = :organizationId
        )
      order by athlete.createdAt desc
      """)
  List<Athlete> findVisibleInOrganization(@Param("organizationId") UUID organizationId);

  Optional<Athlete> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Athlete> findByPerson_IdAndDeletedAtIsNull(UUID personId);

  List<Athlete> findByPerson_IdInAndDeletedAtIsNull(Collection<UUID> personIds);
}
