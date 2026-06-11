package com.karate.tournament.service;

import com.karate.tournament.entity.Athlete;
import com.karate.tournament.dto.request.ClubRosterCreateRequest;
import com.karate.tournament.dto.response.ClubRosterResponse;
import com.karate.tournament.dto.request.ClubRosterUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface ClubRosterService {
  List<ClubRosterResponse> list(UUID organizationId);
  ClubRosterResponse create(UUID organizationId, ClubRosterCreateRequest request);
  ClubRosterResponse update(UUID organizationId, UUID rosterId, ClubRosterUpdateRequest request);
  void delete(UUID organizationId, UUID rosterId);
  void requireAthleteBelongsToOrganization(UUID organizationId, Athlete athlete);
}
