package com.karate.tournament.service;

import com.karate.tournament.dto.request.AnnouncementCreateRequest;
import com.karate.tournament.dto.request.AnnouncementUpdateRequest;
import com.karate.tournament.dto.response.ClubAnnouncementResponse;
import java.util.List;
import java.util.UUID;

public interface ClubAnnouncementService {
  List<ClubAnnouncementResponse> listByOrganization(UUID organizationId);
  List<ClubAnnouncementResponse> listForCurrentUser();
  ClubAnnouncementResponse create(UUID organizationId, AnnouncementCreateRequest request);
  ClubAnnouncementResponse update(UUID organizationId, UUID announcementId, AnnouncementUpdateRequest request);
  void delete(UUID organizationId, UUID announcementId);
}
