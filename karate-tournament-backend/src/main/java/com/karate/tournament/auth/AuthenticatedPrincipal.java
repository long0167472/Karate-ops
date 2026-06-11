package com.karate.tournament.auth;

import com.karate.tournament.entity.enums.SystemRole;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedPrincipal(
    UUID userId,
    UUID primaryOrganizationId,
    String email,
    String displayName,
    Set<SystemRole> roles
) {
  public CurrentActor actor() {
    return new CurrentActor(userId, primaryOrganizationId, roles);
  }
}
