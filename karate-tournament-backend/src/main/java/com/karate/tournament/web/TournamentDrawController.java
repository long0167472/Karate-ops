package com.karate.tournament.web;

import lombok.RequiredArgsConstructor;
import com.karate.tournament.dto.response.TournamentDrawSummaryResponse;
import com.karate.tournament.service.TournamentDrawService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/draw")
@RequiredArgsConstructor
public class TournamentDrawController {

  private final TournamentDrawService drawService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TournamentDrawSummaryResponse generateDraw(@PathVariable UUID tournamentId) {
    return drawService.generateDraw(tournamentId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearDraw(@PathVariable UUID tournamentId) {
    drawService.clearDraw(tournamentId);
  }

  @GetMapping
  public TournamentDrawSummaryResponse getDraw(@PathVariable UUID tournamentId) {
    return drawService.getDraw(tournamentId);
  }

  @PostMapping("/start")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void startTournament(@PathVariable UUID tournamentId) {
    drawService.startTournament(tournamentId);
  }
}
