package com.karate.tournament.web;

import com.karate.tournament.dto.request.DecisionNoteRequest;
import com.karate.tournament.dto.response.TournamentJoinRequestResponse;
import com.karate.tournament.entity.enums.TournamentJoinRequestStatus;
import com.karate.tournament.service.TournamentJoinRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/tournament-join-requests")
@RequiredArgsConstructor
public class TournamentJoinRequestController {
  private final TournamentJoinRequestService joinRequests;

  @GetMapping
  public List<TournamentJoinRequestResponse> list(@PathVariable UUID organizationId) {
    return joinRequests.listByOrganization(organizationId);
  }

  @PatchMapping("/{requestId}/approve")
  public TournamentJoinRequestResponse approve(
      @PathVariable UUID organizationId,
      @PathVariable UUID requestId,
      @Valid @RequestBody(required = false) DecisionNoteRequest request
  ) {
    return joinRequests.decide(organizationId, requestId, TournamentJoinRequestStatus.APPROVED,
        request == null ? null : request.decisionNote());
  }

  @PatchMapping("/{requestId}/reject")
  public TournamentJoinRequestResponse reject(
      @PathVariable UUID organizationId,
      @PathVariable UUID requestId,
      @Valid @RequestBody(required = false) DecisionNoteRequest request
  ) {
    return joinRequests.decide(organizationId, requestId, TournamentJoinRequestStatus.REJECTED,
        request == null ? null : request.decisionNote());
  }
}
