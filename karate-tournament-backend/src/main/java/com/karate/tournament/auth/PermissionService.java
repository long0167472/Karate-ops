package com.karate.tournament.auth;

import com.karate.tournament.entity.Tournament;
import java.util.UUID;

public interface PermissionService {
  CurrentActor currentActor();
  void requireGlobalAdmin();
  void requireTournamentManage(Tournament tournament);
  void requireTournamentCreate(UUID ownerOrganizationId);
  void requireRosterManage(UUID organizationId);
  void requireClubView(UUID organizationId);
  void requireAttendanceManage(UUID organizationId);
  void requireTatamiOperate(Tournament tournament);
  void requireViewTournament(Tournament tournament);
  boolean canManageClub(UUID organizationId);
  boolean canViewClub(UUID organizationId);
  void requireSelfOrClubManage(UUID organizationId, UUID userId);
  void requireMemberSelfView(UUID userId);
}
