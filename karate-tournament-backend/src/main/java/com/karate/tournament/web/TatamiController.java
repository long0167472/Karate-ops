package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.TatamiService;
import com.karate.tournament.dto.request.AssignMatchRequest;
import com.karate.tournament.dto.response.MatchResponse;
import com.karate.tournament.dto.request.TatamiCreateRequest;
import com.karate.tournament.dto.response.TatamiResponse;
import com.karate.tournament.dto.request.TatamiUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class TatamiController {
  private final TatamiService tatamis;

  @GetMapping("/tournaments/{tournamentId}/tatamis")
  public List<TatamiResponse> list(@PathVariable UUID tournamentId) {
    return tatamis.list(tournamentId);
  }

  @PostMapping("/tournaments/{tournamentId}/tatamis")
  @ResponseStatus(HttpStatus.CREATED)
  public TatamiResponse create(@PathVariable UUID tournamentId, @Valid @RequestBody TatamiCreateRequest request) {
    return tatamis.create(tournamentId, request);
  }

  @PatchMapping("/tatamis/{id}")
  public TatamiResponse update(@PathVariable UUID id, @Valid @RequestBody TatamiUpdateRequest request) {
    return tatamis.update(id, request);
  }

  @DeleteMapping("/tatamis/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    tatamis.delete(id);
  }

  @GetMapping("/tatamis/{id}/current-match")
  public ResponseEntity<MatchResponse> currentMatch(@PathVariable UUID id) {
    MatchResponse match = tatamis.currentMatch(id);
    return match == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(match);
  }

  @PostMapping("/tatamis/{id}/assign-match")
  public MatchResponse assignMatch(@PathVariable UUID id, @Valid @RequestBody AssignMatchRequest request) {
    return tatamis.assignMatch(id, request);
  }
}
