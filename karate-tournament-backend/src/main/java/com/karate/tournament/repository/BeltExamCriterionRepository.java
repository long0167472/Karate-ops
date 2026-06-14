package com.karate.tournament.repository;

import com.karate.tournament.entity.BeltExamCriterion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeltExamCriterionRepository extends JpaRepository<BeltExamCriterion, UUID> {
  List<BeltExamCriterion> findByExam_IdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(UUID examId);
  Optional<BeltExamCriterion> findByIdAndDeletedAtIsNull(UUID id);
}
