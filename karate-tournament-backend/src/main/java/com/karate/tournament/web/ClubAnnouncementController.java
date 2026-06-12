package com.karate.tournament.web;

import com.karate.tournament.dto.request.AnnouncementCreateRequest;
import com.karate.tournament.dto.request.AnnouncementUpdateRequest;
import com.karate.tournament.dto.response.ClubAnnouncementResponse;
import com.karate.tournament.service.ClubAnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/announcements")
@RequiredArgsConstructor
public class ClubAnnouncementController {
  private final ClubAnnouncementService announcements;

  @GetMapping
  public List<ClubAnnouncementResponse> list(@PathVariable UUID organizationId) {
    return announcements.listByOrganization(organizationId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClubAnnouncementResponse create(
      @PathVariable UUID organizationId,
      @Valid @RequestBody AnnouncementCreateRequest request
  ) {
    return announcements.create(organizationId, request);
  }

  @PatchMapping("/{announcementId}")
  public ClubAnnouncementResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID announcementId,
      @Valid @RequestBody AnnouncementUpdateRequest request
  ) {
    return announcements.update(organizationId, announcementId, request);
  }

  @DeleteMapping("/{announcementId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID organizationId, @PathVariable UUID announcementId) {
    announcements.delete(organizationId, announcementId);
  }
}
