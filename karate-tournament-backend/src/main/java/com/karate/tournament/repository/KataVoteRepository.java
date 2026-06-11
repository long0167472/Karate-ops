package com.karate.tournament.repository;

import com.karate.tournament.entity.KataVote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KataVoteRepository extends JpaRepository<KataVote, UUID> {
  List<KataVote> findByMatch_IdAndDeletedAtIsNullOrderByJudgeNumberAsc(UUID matchId);

  Optional<KataVote> findByMatch_IdAndJudgeNumberAndDeletedAtIsNull(UUID matchId, Integer judgeNumber);
}
