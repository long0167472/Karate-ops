package com.karate.tournament.repository;

import com.karate.tournament.entity.UserRoleAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
  List<UserRoleAssignment> findByUser_IdAndDeletedAtIsNull(UUID userId);

  Optional<UserRoleAssignment> findByUser_IdAndRole_CodeAndScopeTypeAndScopeIdAndDeletedAtIsNull(
      UUID userId,
      String roleCode,
      String scopeType,
      UUID scopeId
  );
}
