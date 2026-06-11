package com.karate.tournament.auth;

import com.karate.tournament.entity.enums.SystemRole;
import java.util.Set;
import java.util.UUID;

public record CurrentActor(
    UUID userId,
    UUID primaryOrganizationId,
    Set<SystemRole> roles
) {
  public boolean hasRole(SystemRole role) {
    return roles.contains(role);
  }
}
