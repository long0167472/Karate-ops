package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.AthleteService;
import com.karate.tournament.dto.request.AthleteCreateRequest;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.dto.request.AthleteUpdateRequest;
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
@RequestMapping("/api/athletes")
@RequiredArgsConstructor
public class AthleteController {
  private final AthleteService athletes;

  @GetMapping
  public List<AthleteResponse> list() {
    return athletes.list();
  }

  @GetMapping("/{id}")
  public AthleteResponse get(@PathVariable UUID id) {
    return athletes.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AthleteResponse create(@Valid @RequestBody AthleteCreateRequest request) {
    return athletes.create(request);
  }

  @PatchMapping("/{id}")
  public AthleteResponse update(@PathVariable UUID id, @Valid @RequestBody AthleteUpdateRequest request) {
    return athletes.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    athletes.delete(id);
  }
}
