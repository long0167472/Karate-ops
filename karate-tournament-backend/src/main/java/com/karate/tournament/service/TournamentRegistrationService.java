package com.karate.tournament.service;

import com.karate.tournament.dto.request.RegisterAthleteRequest;
import com.karate.tournament.dto.request.RegisterClubRequest;
import com.karate.tournament.dto.response.TournamentRegistrationResponse;
import java.util.UUID;

public interface TournamentRegistrationService {
  TournamentRegistrationResponse registerClub(UUID tournamentId, RegisterClubRequest req);
  void withdrawClub(UUID tournamentId);
  TournamentRegistrationResponse getMyRegistration(UUID tournamentId);
  TournamentRegistrationResponse addAthlete(UUID tournamentId, RegisterAthleteRequest req);
  void removeAthlete(UUID tournamentId, UUID entryId);
}
