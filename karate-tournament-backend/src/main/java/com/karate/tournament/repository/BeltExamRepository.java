package com.karate.tournament.repository;

import com.karate.tournament.entity.BeltExam;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeltExamRepository extends JpaRepository<BeltExam, UUID> {
  List<BeltExam> findByOrganization_IdAndDeletedAtIsNullOrderByExamDateDescCreatedAtDesc(UUID organizationId);
  Optional<BeltExam> findByIdAndDeletedAtIsNull(UUID id);
}
