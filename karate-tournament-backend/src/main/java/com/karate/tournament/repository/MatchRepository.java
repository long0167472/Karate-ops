package com.karate.tournament.repository;

import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.enums.MatchStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, UUID> {
  Optional<Match> findByIdAndDeletedAtIsNull(UUID id);

  List<Match> findByTournament_IdAndDeletedAtIsNullOrderByScheduledAtAscMatchNumberAsc(UUID tournamentId);

  List<Match> findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(UUID categoryId);

  List<Match> findByTeamMatchGroupIdAndDeletedAtIsNullOrderByTeamBoutOrderAscMatchNumberAsc(UUID teamMatchGroupId);

  List<Match> findByTatami_IdAndDeletedAtIsNullAndStatusInOrderByScheduledAtAscMatchNumberAsc(
      UUID tatamiId,
      Collection<MatchStatus> statuses
  );

  long countByTournament_IdAndDeletedAtIsNull(UUID tournamentId);

  long countByTournament_IdAndDeletedAtIsNullAndStatusIn(UUID tournamentId, Collection<MatchStatus> statuses);

  @Query("""
      select m.status as status, count(m) as total
      from Match m
      where m.tournament.id = :tournamentId and m.deletedAt is null
      group by m.status
      order by m.status
      """)
  List<MatchStatusCountProjection> countByStatus(@Param("tournamentId") UUID tournamentId);

  interface MatchStatusCountProjection {
    MatchStatus getStatus();

    Long getTotal();
  }
}
