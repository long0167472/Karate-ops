package com.karate.tournament.repository;

import com.karate.tournament.entity.Entry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntryRepository extends JpaRepository<Entry, UUID> {
  List<Entry> findByCategory_IdAndDeletedAtIsNullOrderBySeedNoAscCreatedAtAsc(UUID categoryId);

  @Query("""
      select e
      from Entry e
      where e.category.tournament.id = :tournamentId
        and e.deletedAt is null
        and e.category.deletedAt is null
      order by e.category.name asc, e.seedNo asc, e.createdAt asc
      """)
  List<Entry> findByTournament(@Param("tournamentId") UUID tournamentId);

  Optional<Entry> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Entry> findByCategory_IdAndAthlete_IdAndDeletedAtIsNull(UUID categoryId, UUID athleteId);

  long countByCategory_IdAndDeletedAtIsNull(UUID categoryId);

  long countByCategory_IdAndTournamentParticipant_Organization_IdAndDeletedAtIsNull(UUID categoryId, UUID organizationId);

  @Query("""
      select count(distinct e.athlete.id)
      from Entry e
      where e.category.tournament.id = :tournamentId
        and e.deletedAt is null
        and e.category.deletedAt is null
        and e.athlete is not null
      """)
  long countDistinctAthletesByTournament(@Param("tournamentId") UUID tournamentId);

  @Query("""
      select count(e)
      from Entry e
      where e.tournamentParticipant.organization.id = :organizationId
        and e.deletedAt is null
        and e.tournamentParticipant.deletedAt is null
      """)
  long countTournamentEntriesByOrganization(@Param("organizationId") UUID organizationId);

  @Query("""
      select count(e)
      from Entry e
      where e.tournamentParticipant.organization.id = :organizationId
        and e.athlete.id = :athleteId
        and e.deletedAt is null
        and e.tournamentParticipant.deletedAt is null
      """)
  long countTournamentEntriesByOrganizationAndAthlete(
      @Param("organizationId") UUID organizationId,
      @Param("athleteId") UUID athleteId
  );

  @Query("""
      select distinct e.category.tournament.name
      from Entry e
      where e.tournamentParticipant.organization.id = :organizationId
        and e.athlete.id = :athleteId
        and e.deletedAt is null
        and e.tournamentParticipant.deletedAt is null
        and e.category.tournament.deletedAt is null
      order by e.category.tournament.name asc
      """)
  List<String> findTournamentNamesByOrganizationAndAthlete(
      @Param("organizationId") UUID organizationId,
      @Param("athleteId") UUID athleteId
  );
}
