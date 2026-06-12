package com.karate.tournament.service;

import com.karate.tournament.dto.response.PublicTournamentSummaryResponse;
import java.util.List;

public interface PublicTournamentService {

  /**
   * List tournaments by phase:
   *   UPCOMING  — status IN (DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED)
   *   ONGOING   — status = RUNNING
   *   FINISHED  — status IN (COMPLETED, ARCHIVED)
   */
  List<PublicTournamentSummaryResponse> listPublic(String phase);
}
