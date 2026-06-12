package com.karate.tournament.web;

import com.karate.tournament.dto.request.RegisterAthleteRequest;
import com.karate.tournament.dto.request.RegisterClubRequest;
import com.karate.tournament.dto.response.TournamentRegistrationResponse;
import com.karate.tournament.service.TournamentRegistrationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/registration")
@RequiredArgsConstructor
public class TournamentRegistrationController {

  private final TournamentRegistrationService registrationService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TournamentRegistrationResponse registerClub(
      @PathVariable UUID tournamentId,
      @RequestBody RegisterClubRequest req) {
    return registrationService.registerClub(tournamentId, req);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void withdrawClub(@PathVariable UUID tournamentId) {
    registrationService.withdrawClub(tournamentId);
  }

  @GetMapping
  public TournamentRegistrationResponse getMyRegistration(@PathVariable UUID tournamentId) {
    return registrationService.getMyRegistration(tournamentId);
  }

  @PostMapping("/athletes")
  @ResponseStatus(HttpStatus.CREATED)
  public TournamentRegistrationResponse addAthlete(
      @PathVariable UUID tournamentId,
      @RequestBody RegisterAthleteRequest req) {
    return registrationService.addAthlete(tournamentId, req);
  }

  @DeleteMapping("/athletes/{entryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeAthlete(
      @PathVariable UUID tournamentId,
      @PathVariable UUID entryId) {
    registrationService.removeAthlete(tournamentId, entryId);
  }
}
