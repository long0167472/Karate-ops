package com.karate.tournament.web;

import com.karate.tournament.dto.request.PersonUpdateRequest;
import com.karate.tournament.dto.response.PersonResponse;
import com.karate.tournament.service.PersonService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/persons")
@RequiredArgsConstructor
public class OrganizationPersonController {
  private final PersonService persons;

  @PatchMapping("/{personId}")
  public PersonResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID personId,
      @Valid @RequestBody PersonUpdateRequest request
  ) {
    return persons.updateInOrganization(organizationId, personId, request);
  }
}
