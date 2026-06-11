package com.karate.tournament.repository;

import com.karate.tournament.entity.Athlete;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteRepository extends JpaRepository<Athlete, UUID> {
  List<Athlete> findByDeletedAtIsNullOrderByCreatedAtDesc();

  Optional<Athlete> findByIdAndDeletedAtIsNull(UUID id);

  Optional<Athlete> findByPerson_IdAndDeletedAtIsNull(UUID personId);
}
