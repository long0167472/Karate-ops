package com.karate.tournament.repository;

import com.karate.tournament.entity.CategoryResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryResultRepository extends JpaRepository<CategoryResult, UUID> {
  boolean existsByCategory_IdAndEntry_IdAndDeletedAtIsNull(UUID categoryId, UUID entryId);

  @Query("""
      select cr.entry.tournamentParticipant.organization.id as organizationId,
             cr.entry.tournamentParticipant.organization.name as organizationName,
             sum(case when cr.medal = 'GOLD' then 1 else 0 end) as gold,
             sum(case when cr.medal = 'SILVER' then 1 else 0 end) as silver,
             sum(case when cr.medal = 'BRONZE' then 1 else 0 end) as bronze,
             count(cr) as total
      from CategoryResult cr
      where cr.category.tournament.id = :tournamentId and cr.deletedAt is null
      group by cr.entry.tournamentParticipant.organization.id, cr.entry.tournamentParticipant.organization.name
      order by gold desc, silver desc, bronze desc, total desc, cr.entry.tournamentParticipant.organization.name asc
      """)
  List<MedalTableProjection> medalTable(@Param("tournamentId") UUID tournamentId);

  interface MedalTableProjection {
    UUID getOrganizationId();

    String getOrganizationName();

    Long getGold();

    Long getSilver();

    Long getBronze();

    Long getTotal();
  }
}
