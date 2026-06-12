package com.karate.tournament.service;

import com.karate.tournament.dto.response.AthleteRankingResponse;
import com.karate.tournament.dto.response.ClubStandingResponse;
import java.util.List;
import java.util.UUID;

public interface TournamentPointsService {
  void awardMatchPoints(UUID matchId, UUID winnerEntryId, int roundNumber);
  List<ClubStandingResponse> getClubStandings(UUID tournamentId);
  List<AthleteRankingResponse> getAthleteRanking(UUID tournamentId);
}
