package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.MatchService;
import com.karate.tournament.dto.request.ConfirmResultRequest;
import com.karate.tournament.dto.request.MatchEventRequest;
import com.karate.tournament.dto.response.MatchResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {
  private final MatchService matches;

  @GetMapping("/tournaments/{tournamentId}/matches")
  public List<MatchResponse> listByTournament(@PathVariable UUID tournamentId) {
    return matches.listByTournament(tournamentId);
  }

  @GetMapping("/matches/{id}")
  public MatchResponse get(@PathVariable UUID id) {
    return matches.get(id);
  }

  @PostMapping("/matches/{id}/events")
  public MatchResponse recordEvent(@PathVariable UUID id, @Valid @RequestBody MatchEventRequest request) {
    return matches.recordEvent(id, request);
  }

  @PostMapping("/matches/{id}/result")
  public MatchResponse confirmResult(@PathVariable UUID id, @Valid @RequestBody ConfirmResultRequest request) {
    return matches.confirmResult(id, request);
  }
}
