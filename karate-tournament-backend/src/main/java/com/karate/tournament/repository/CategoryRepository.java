package com.karate.tournament.repository;

import com.karate.tournament.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  List<Category> findByTournament_IdAndDeletedAtIsNullOrderByNameAsc(UUID tournamentId);

  Optional<Category> findByIdAndDeletedAtIsNull(UUID id);

  long countByTournament_IdAndDeletedAtIsNull(UUID tournamentId);
}
