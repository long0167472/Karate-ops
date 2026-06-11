package com.karate.tournament.service;

import com.karate.tournament.entity.Tatami;
import com.karate.tournament.dto.request.AssignMatchRequest;
import com.karate.tournament.dto.response.MatchResponse;
import com.karate.tournament.dto.request.TatamiCreateRequest;
import com.karate.tournament.dto.response.TatamiResponse;
import com.karate.tournament.dto.request.TatamiUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface TatamiService {
  List<TatamiResponse> list(UUID tournamentId);
  TatamiResponse create(UUID tournamentId, TatamiCreateRequest request);
  TatamiResponse update(UUID id, TatamiUpdateRequest request);
  void delete(UUID id);
  MatchResponse currentMatch(UUID tatamiId);
  MatchResponse assignMatch(UUID tatamiId, AssignMatchRequest request);
  Tatami requireTatami(UUID id);
}
