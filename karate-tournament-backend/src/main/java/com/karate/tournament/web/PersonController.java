package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.PersonService;
import com.karate.tournament.dto.request.PersonCreateRequest;
import com.karate.tournament.dto.response.PersonResponse;
import com.karate.tournament.dto.request.PersonUpdateRequest;
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
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {
  private final PersonService persons;

  @GetMapping
  public List<PersonResponse> list() {
    return persons.list();
  }

  @GetMapping("/{id}")
  public PersonResponse get(@PathVariable UUID id) {
    return persons.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PersonResponse create(@Valid @RequestBody PersonCreateRequest request) {
    return persons.create(request);
  }

  @PatchMapping("/{id}")
  public PersonResponse update(@PathVariable UUID id, @Valid @RequestBody PersonUpdateRequest request) {
    return persons.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    persons.delete(id);
  }
}
