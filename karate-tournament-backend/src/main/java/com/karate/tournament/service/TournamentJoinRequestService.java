package com.karate.tournament.service;

import com.karate.tournament.dto.request.TournamentJoinRequestCreateRequest;
import com.karate.tournament.dto.response.TournamentJoinRequestResponse;
import com.karate.tournament.entity.enums.TournamentJoinRequestStatus;
import java.util.List;
import java.util.UUID;

public interface TournamentJoinRequestService {
  TournamentJoinRequestResponse createForCurrentUser(TournamentJoinRequestCreateRequest request);
  List<TournamentJoinRequestResponse> listForCurrentUser();
  List<TournamentJoinRequestResponse> listByOrganization(UUID organizationId);
  TournamentJoinRequestResponse decide(UUID organizationId, UUID requestId, TournamentJoinRequestStatus status, String decisionNote);
}
