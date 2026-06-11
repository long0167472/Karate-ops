package com.karate.tournament.repository;

import com.karate.tournament.entity.Tournament;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, UUID> {
  List<Tournament> findByDeletedAtIsNullOrderByStartsOnDescCreatedAtDesc();

  Optional<Tournament> findByIdAndDeletedAtIsNull(UUID id);
}
