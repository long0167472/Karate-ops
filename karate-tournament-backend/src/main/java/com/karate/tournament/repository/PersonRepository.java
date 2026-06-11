package com.karate.tournament.repository;

import com.karate.tournament.entity.Person;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, UUID> {
  List<Person> findByDeletedAtIsNullOrderByDisplayNameAsc();

  Optional<Person> findByIdAndDeletedAtIsNull(UUID id);
}
