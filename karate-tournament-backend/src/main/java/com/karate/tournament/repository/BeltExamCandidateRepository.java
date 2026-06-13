package com.karate.tournament.repository;

import com.karate.tournament.entity.BeltExamCandidate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeltExamCandidateRepository extends JpaRepository<BeltExamCandidate, UUID> {
  List<BeltExamCandidate> findByExam_IdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID examId);
  Optional<BeltExamCandidate> findByIdAndDeletedAtIsNull(UUID id);
  Optional<BeltExamCandidate> findByExam_IdAndOrganizationMember_IdAndDeletedAtIsNull(UUID examId, UUID memberId);
  Optional<BeltExamCandidate> findByExam_IdAndAthlete_IdAndDeletedAtIsNull(UUID examId, UUID athleteId);
}
