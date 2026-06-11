package com.karate.tournament.service;

import com.karate.tournament.entity.Match;
import com.karate.tournament.dto.request.ConfirmResultRequest;
import com.karate.tournament.dto.request.MatchEventRequest;
import com.karate.tournament.dto.response.MatchResponse;
import java.util.List;
import java.util.UUID;

public interface MatchService {
  List<MatchResponse> listByTournament(UUID tournamentId);
  MatchResponse get(UUID id);
  MatchResponse recordEvent(UUID matchId, MatchEventRequest request);
  MatchResponse confirmResult(UUID matchId, ConfirmResultRequest request);
  Match requireMatch(UUID id);
}
