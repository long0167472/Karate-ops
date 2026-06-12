package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.TournamentService;
import com.karate.tournament.dto.request.ParticipantCreateRequest;
import com.karate.tournament.dto.request.ParticipantStatusRequest;
import com.karate.tournament.dto.request.TournamentCreateRequest;
import com.karate.tournament.dto.response.TournamentParticipantResponse;
import com.karate.tournament.dto.response.TournamentResponse;
import com.karate.tournament.dto.request.TournamentUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {
  private final TournamentService tournaments;

  @GetMapping
  public List<TournamentResponse> list() {
    return tournaments.list();
  }

  @GetMapping("/{id}")
  public TournamentResponse get(@PathVariable UUID id) {
    return tournaments.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TournamentResponse create(@Valid @RequestBody TournamentCreateRequest request) {
    return tournaments.create(request);
  }

  @PatchMapping("/{id}")
  public TournamentResponse update(@PathVariable UUID id, @Valid @RequestBody TournamentUpdateRequest request) {
    return tournaments.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    tournaments.delete(id);
  }

  @GetMapping("/{id}/participants")
  public List<TournamentParticipantResponse> participants(@PathVariable UUID id) {
    return tournaments.listParticipants(id);
  }

  @PostMapping("/{id}/participants")
  @ResponseStatus(HttpStatus.CREATED)
  public TournamentParticipantResponse addParticipant(
      @PathVariable UUID id,
      @Valid @RequestBody ParticipantCreateRequest request
  ) {
    return tournaments.addParticipant(id, request);
  }

  @PostMapping("/{id}/advance-step")
  public TournamentResponse advanceStep(@PathVariable UUID id) {
    return tournaments.advanceStep(id);
  }

  @PostMapping("/{id}/regress-step")
  public TournamentResponse regressStep(@PathVariable UUID id) {
    return tournaments.regressStep(id);
  }

  @PostMapping("/{id}/open-registration")
  public TournamentResponse openRegistration(@PathVariable UUID id) {
    return tournaments.openRegistration(id);
  }

  @PostMapping("/{id}/close-registration")
  public TournamentResponse closeRegistration(@PathVariable UUID id) {
    return tournaments.closeRegistration(id);
  }

  @PatchMapping("/{id}/participants/{participantId}/status")
  public TournamentParticipantResponse updateParticipantStatus(
      @PathVariable UUID id,
      @PathVariable UUID participantId,
      @Valid @RequestBody ParticipantStatusRequest request
  ) {
    return tournaments.updateParticipantStatus(id, participantId, request);
  }
}
