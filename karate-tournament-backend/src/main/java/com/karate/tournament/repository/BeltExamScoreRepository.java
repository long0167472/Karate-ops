package com.karate.tournament.repository;

import com.karate.tournament.entity.BeltExamScore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeltExamScoreRepository extends JpaRepository<BeltExamScore, UUID> {
  List<BeltExamScore> findByCandidate_IdAndDeletedAtIsNull(UUID candidateId);
  Optional<BeltExamScore> findByCandidate_IdAndCriterion_IdAndDeletedAtIsNull(UUID candidateId, UUID criterionId);
}
