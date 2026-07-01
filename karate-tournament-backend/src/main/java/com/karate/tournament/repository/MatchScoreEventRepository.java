package com.karate.tournament.repository;

import com.karate.tournament.entity.MatchScoreEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchScoreEventRepository extends JpaRepository<MatchScoreEvent, UUID> {
  List<MatchScoreEvent> findByMatch_IdAndDeletedAtIsNullOrderByOccurredAtAsc(UUID matchId);

  List<MatchScoreEvent> findTop80ByMatch_IdAndDeletedAtIsNullOrderByOccurredAtDesc(UUID matchId);
}
