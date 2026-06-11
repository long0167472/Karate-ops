package com.karate.tournament.repository;

import com.karate.tournament.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCodeAndDeletedAtIsNull(String code);
}
