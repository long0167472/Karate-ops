package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubTrainingSchedule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubTrainingScheduleRepository extends JpaRepository<ClubTrainingSchedule, UUID> {
  Optional<ClubTrainingSchedule> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId);

  List<ClubTrainingSchedule> findByActiveTrueAndDeletedAtIsNull();
}
