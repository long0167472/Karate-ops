package com.karate.tournament.web;

import com.karate.tournament.dto.request.AthleteCreateRequest;
import com.karate.tournament.dto.request.AthleteUpdateRequest;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.service.AthleteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/athletes")
@RequiredArgsConstructor
public class OrganizationAthleteController {
  private final AthleteService athletes;

  @GetMapping
  public List<AthleteResponse> list(@PathVariable UUID organizationId) {
    return athletes.listByOrganization(organizationId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AthleteResponse create(@PathVariable UUID organizationId, @Valid @RequestBody AthleteCreateRequest request) {
    return athletes.createInOrganization(organizationId, request);
  }

  @PatchMapping("/{athleteId}")
  public AthleteResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID athleteId,
      @Valid @RequestBody AthleteUpdateRequest request
  ) {
    return athletes.updateInOrganization(organizationId, athleteId, request);
  }
}
