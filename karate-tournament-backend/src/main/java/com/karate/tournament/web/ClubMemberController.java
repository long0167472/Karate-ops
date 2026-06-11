package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.OrganizationMemberService;
import com.karate.tournament.dto.request.ClubMemberCreateRequest;
import com.karate.tournament.dto.response.ClubMemberResponse;
import com.karate.tournament.dto.request.ClubMemberUpdateRequest;
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
@RequestMapping("/api/organizations/{organizationId}/members")
@RequiredArgsConstructor
public class ClubMemberController {
  private final OrganizationMemberService members;

  @GetMapping
  public List<ClubMemberResponse> list(@PathVariable UUID organizationId) {
    return members.list(organizationId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClubMemberResponse create(
      @PathVariable UUID organizationId,
      @Valid @RequestBody ClubMemberCreateRequest request
  ) {
    return members.create(organizationId, request);
  }

  @PatchMapping("/{memberId}")
  public ClubMemberResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID memberId,
      @Valid @RequestBody ClubMemberUpdateRequest request
  ) {
    return members.update(organizationId, memberId, request);
  }

  @DeleteMapping("/{memberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID organizationId, @PathVariable UUID memberId) {
    members.delete(organizationId, memberId);
  }
}
