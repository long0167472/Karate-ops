package com.karate.tournament.web;

import com.karate.tournament.dto.response.PublicTournamentSummaryResponse;
import com.karate.tournament.service.PublicTournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/tournaments")
@RequiredArgsConstructor
public class PublicTournamentController {

  private final PublicTournamentService publicTournaments;

  @GetMapping
  public List<PublicTournamentSummaryResponse> list(
      @RequestParam(name = "phase", defaultValue = "UPCOMING") String phase
  ) {
    return publicTournaments.listPublic(phase);
  }
}
