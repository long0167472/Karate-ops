package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.ClubRosterService;
import com.karate.tournament.dto.request.ClubRosterCreateRequest;
import com.karate.tournament.dto.response.ClubRosterResponse;
import com.karate.tournament.dto.request.ClubRosterUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/organizations/{organizationId}/roster")
@RequiredArgsConstructor
public class ClubRosterController {
  private final ClubRosterService roster;

  @GetMapping
  public List<ClubRosterResponse> list(@PathVariable UUID organizationId) {
    return roster.list(organizationId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClubRosterResponse create(
      @PathVariable UUID organizationId,
      @Valid @RequestBody ClubRosterCreateRequest request
  ) {
    return roster.create(organizationId, request);
  }

  @PatchMapping("/{rosterId}")
  public ClubRosterResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID rosterId,
      @Valid @RequestBody ClubRosterUpdateRequest request
  ) {
    return roster.update(organizationId, rosterId, request);
  }

  @DeleteMapping("/{rosterId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID organizationId, @PathVariable UUID rosterId) {
    roster.delete(organizationId, rosterId);
  }
}
