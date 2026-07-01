package com.karate.tournament.service.impl;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.AnnouncementCreateRequest;
import com.karate.tournament.dto.request.AnnouncementUpdateRequest;
import com.karate.tournament.dto.response.ClubAnnouncementResponse;
import com.karate.tournament.entity.ClubAnnouncement;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.ClubAnnouncementRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.service.ClubAnnouncementService;
import com.karate.tournament.web.ApiMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubAnnouncementServiceImpl implements ClubAnnouncementService {
  private final ClubAnnouncementRepository announcements;
  private final OrganizationRepository organizations;
  private final OrganizationMemberRepository members;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<ClubAnnouncementResponse> listByOrganization(UUID organizationId) {
    permissions.requireClubView(organizationId);
    return announcements.findByOrganization_IdAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::announcement)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClubAnnouncementResponse> listForCurrentUser() {
    CurrentActor actor = permissions.currentActor();
    permissions.requireMemberSelfView(actor.userId());
    List<UUID> organizationIds = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId())
        .stream()
        .map(member -> member.organization.id)
        .distinct()
        .toList();
    if (organizationIds.isEmpty()) {
      return List.of();
    }
    return announcements.findByOrganization_IdInAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(organizationIds)
        .stream()
        .map(mapper::announcement)
        .toList();
  }

  @Transactional
  public ClubAnnouncementResponse create(UUID organizationId, AnnouncementCreateRequest request) {
    permissions.requireRosterManage(organizationId);
    Organization organization = organizations.findByIdAndDeletedAtIsNull(organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    ClubAnnouncement announcement = ClubAnnouncement.create();
    announcement.organization = organization;
    announcement.createdByUser = users.findByIdAndDeletedAtIsNull(permissions.currentActor().userId()).orElse(null);
    announcement.title = request.title().trim();
    announcement.content = request.content().trim();
    announcement.pinned = Boolean.TRUE.equals(request.pinned());
    return mapper.announcement(announcements.save(announcement));
  }

  @Transactional
  public ClubAnnouncementResponse update(UUID organizationId, UUID announcementId, AnnouncementUpdateRequest request) {
    permissions.requireRosterManage(organizationId);
    ClubAnnouncement announcement = requireAnnouncement(organizationId, announcementId);
    if (request.title() != null && !request.title().isBlank()) {
      announcement.title = request.title().trim();
    }
    if (request.content() != null && !request.content().isBlank()) {
      announcement.content = request.content().trim();
    }
    if (request.pinned() != null) {
      announcement.pinned = request.pinned();
    }
    return mapper.announcement(announcement);
  }

  @Transactional
  public void delete(UUID organizationId, UUID announcementId) {
    permissions.requireRosterManage(organizationId);
    requireAnnouncement(organizationId, announcementId).softDelete();
  }

  private ClubAnnouncement requireAnnouncement(UUID organizationId, UUID announcementId) {
    ClubAnnouncement announcement = announcements.findByIdAndDeletedAtIsNull(announcementId)
        .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
    if (!announcement.organization.id.equals(organizationId)) {
      throw new ResourceNotFoundException("Announcement does not belong to organization");
    }
    return announcement;
  }
}
