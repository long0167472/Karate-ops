package com.karate.tournament.repository;

import com.karate.tournament.entity.Bracket;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BracketRepository extends JpaRepository<Bracket, UUID> {
  List<Bracket> findByCategory_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID categoryId);

  Optional<Bracket> findByIdAndDeletedAtIsNull(UUID id);
}
