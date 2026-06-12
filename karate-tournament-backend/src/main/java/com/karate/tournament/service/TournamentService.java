package com.karate.tournament.service;

import com.karate.tournament.entity.Tournament;
import com.karate.tournament.dto.request.ParticipantCreateRequest;
import com.karate.tournament.dto.request.ParticipantStatusRequest;
import com.karate.tournament.dto.request.TournamentCreateRequest;
import com.karate.tournament.dto.response.TournamentParticipantResponse;
import com.karate.tournament.dto.response.TournamentResponse;
import com.karate.tournament.dto.request.TournamentUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface TournamentService {
  List<TournamentResponse> list();
  TournamentResponse get(UUID id);
  TournamentResponse create(TournamentCreateRequest request);
  TournamentResponse update(UUID id, TournamentUpdateRequest request);
  void delete(UUID id);
  List<TournamentParticipantResponse> listParticipants(UUID tournamentId);
  TournamentParticipantResponse addParticipant(UUID tournamentId, ParticipantCreateRequest request);
  TournamentParticipantResponse updateParticipantStatus(UUID tournamentId, UUID participantId, ParticipantStatusRequest request);
  Tournament requireTournament(UUID id);
  TournamentResponse advanceStep(UUID id);
  TournamentResponse regressStep(UUID id);
  TournamentResponse openRegistration(UUID id);
  TournamentResponse closeRegistration(UUID id);
}
