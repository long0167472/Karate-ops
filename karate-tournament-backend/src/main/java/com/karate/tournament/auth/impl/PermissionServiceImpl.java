package com.karate.tournament.auth.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.auth.*;
import com.karate.tournament.exception.ForbiddenException;

import com.karate.tournament.entity.enums.SystemRole;
import com.karate.tournament.entity.Tournament;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
  private final CurrentActorProvider currentActorProvider;

  public CurrentActor currentActor() {
    return currentActorProvider.currentActor();
  }

  public void requireGlobalAdmin() {
    CurrentActor actor = currentActor();
    if (!actor.hasRole(SystemRole.GLOBAL_ADMIN)) {
      throw new ForbiddenException("GLOBAL_ADMIN role is required");
    }
  }

  public void requireTournamentManage(Tournament tournament) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN)) {
      return;
    }
    if ((actor.hasRole(SystemRole.TOURNAMENT_OWNER) || actor.hasRole(SystemRole.CLUB_MANAGER))
        && tournament.ownerOrganization != null
        && tournament.ownerOrganization.id.equals(actor.primaryOrganizationId())) {
      return;
    }
    throw new ForbiddenException("Tournament management permission is required");
  }

  public void requireTournamentCreate(UUID ownerOrganizationId) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN)) {
      return;
    }
    if ((actor.hasRole(SystemRole.CLUB_MANAGER) || actor.hasRole(SystemRole.TOURNAMENT_OWNER))
        && ownerOrganizationId != null
        && ownerOrganizationId.equals(actor.primaryOrganizationId())) {
      return;
    }
    throw new ForbiddenException("Tournament creation permission is required");
  }

  public void requireRosterManage(UUID organizationId) {
    if (canManageClub(organizationId)) {
      return;
    }
    throw new ForbiddenException("Club roster permission is required");
  }

  public void requireClubView(UUID organizationId) {
    if (canViewClub(organizationId)) {
      return;
    }
    throw new ForbiddenException("Club view permission is required");
  }

  public void requireAttendanceManage(UUID organizationId) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN)) {
      return;
    }
    if ((actor.hasRole(SystemRole.CLUB_MANAGER) || actor.hasRole(SystemRole.COACH))
        && organizationId != null
        && organizationId.equals(actor.primaryOrganizationId())) {
      return;
    }
    throw new ForbiddenException("Club attendance permission is required");
  }

  public void requireTatamiOperate(Tournament tournament) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN) || actor.hasRole(SystemRole.TATAMI_OPERATOR)) {
      return;
    }
    requireTournamentManage(tournament);
  }

  public void requireViewTournament(Tournament tournament) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN)
        || actor.hasRole(SystemRole.VIEWER)
        || actor.hasRole(SystemRole.TATAMI_OPERATOR)
        || actor.hasRole(SystemRole.JUDGE)) {
      return;
    }
    requireTournamentManage(tournament);
  }

  public boolean canManageClub(UUID organizationId) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN)) {
      return true;
    }
    return actor.hasRole(SystemRole.CLUB_MANAGER)
        && organizationId != null
        && organizationId.equals(actor.primaryOrganizationId());
  }

  public boolean canViewClub(UUID organizationId) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN) || actor.hasRole(SystemRole.VIEWER)) {
      return true;
    }
    return (actor.hasRole(SystemRole.CLUB_MANAGER) || actor.hasRole(SystemRole.COACH))
        && organizationId != null
        && organizationId.equals(actor.primaryOrganizationId());
  }

  public void requireSelfOrClubManage(UUID organizationId, UUID userId) {
    CurrentActor actor = currentActor();
    if (canManageClub(organizationId) || actor.userId().equals(userId)) {
      return;
    }
    throw new ForbiddenException("Self or club management permission is required");
  }

  public void requireMemberSelfView(UUID userId) {
    CurrentActor actor = currentActor();
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN) || actor.userId().equals(userId)) {
      return;
    }
    throw new ForbiddenException("Member self-view permission is required");
  }
}
