package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.OrganizationService;
import com.karate.tournament.dto.request.OrganizationCreateRequest;
import com.karate.tournament.dto.response.ManagedClubResponse;
import com.karate.tournament.dto.response.OrganizationResponse;
import com.karate.tournament.dto.request.OrganizationUpdateRequest;
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
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {
  private final OrganizationService organizations;

  @GetMapping
  public List<OrganizationResponse> list() {
    return organizations.list();
  }

  @GetMapping("/managed-clubs")
  public List<ManagedClubResponse> managedClubs() {
    return organizations.managedClubs();
  }

  @GetMapping("/{id}")
  public OrganizationResponse get(@PathVariable UUID id) {
    return organizations.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrganizationResponse create(@Valid @RequestBody OrganizationCreateRequest request) {
    return organizations.create(request);
  }

  @PatchMapping("/{id}")
  public OrganizationResponse update(@PathVariable UUID id, @Valid @RequestBody OrganizationUpdateRequest request) {
    return organizations.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    organizations.delete(id);
  }
}
