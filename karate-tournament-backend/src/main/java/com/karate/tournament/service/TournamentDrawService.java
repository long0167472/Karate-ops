package com.karate.tournament.service;

import com.karate.tournament.dto.response.TournamentDrawSummaryResponse;
import java.util.UUID;

public interface TournamentDrawService {
  TournamentDrawSummaryResponse generateDraw(UUID tournamentId);

  void clearDraw(UUID tournamentId);

  TournamentDrawSummaryResponse getDraw(UUID tournamentId);

  void startTournament(UUID tournamentId);
}
