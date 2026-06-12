package com.karate.tournament.web;

import lombok.RequiredArgsConstructor;
import com.karate.tournament.dto.response.AthleteRankingResponse;
import com.karate.tournament.dto.response.ClubStandingResponse;
import com.karate.tournament.service.TournamentPointsService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/standings")
@RequiredArgsConstructor
public class TournamentStandingsController {

  private final TournamentPointsService tournamentPoints;

  @GetMapping("/clubs")
  public List<ClubStandingResponse> getClubStandings(@PathVariable UUID tournamentId) {
    return tournamentPoints.getClubStandings(tournamentId);
  }

  @GetMapping("/athletes")
  public List<AthleteRankingResponse> getAthleteRanking(@PathVariable UUID tournamentId) {
    return tournamentPoints.getAthleteRanking(tournamentId);
  }
}
