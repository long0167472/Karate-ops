package com.karate.tournament.repository;

import com.karate.tournament.entity.Tatami;
import com.karate.tournament.entity.enums.MatchStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TatamiRepository extends JpaRepository<Tatami, UUID> {
  List<Tatami> findByTournament_IdAndDeletedAtIsNullOrderByTatamiNoAsc(UUID tournamentId);

  Optional<Tatami> findByIdAndDeletedAtIsNull(UUID id);

  @Query("""
      select t.id as tatamiId,
             t.tatamiNo as tatamiNo,
             t.name as name,
             coalesce(sum(case when m.status = :scheduledStatus then 1 else 0 end), 0) as scheduled,
             coalesce(sum(case when m.status in :runningStatuses then 1 else 0 end), 0) as running,
             coalesce(sum(case when m.status in :completedStatuses then 1 else 0 end), 0) as completed,
             t.currentMatch.id as currentMatchId
      from Tatami t
      left join Match m on m.tatami = t and m.deletedAt is null
      where t.tournament.id = :tournamentId and t.deletedAt is null
      group by t.id, t.tatamiNo, t.name, t.currentMatch.id
      order by t.tatamiNo asc
      """)
  List<TatamiDashboardProjection> dashboardRows(
      @Param("tournamentId") UUID tournamentId,
      @Param("scheduledStatus") MatchStatus scheduledStatus,
      @Param("runningStatuses") Collection<MatchStatus> runningStatuses,
      @Param("completedStatuses") Collection<MatchStatus> completedStatuses
  );

  interface TatamiDashboardProjection {
    UUID getTatamiId();

    Integer getTatamiNo();

    String getName();

    Long getScheduled();

    Long getRunning();

    Long getCompleted();

    UUID getCurrentMatchId();
  }
}
