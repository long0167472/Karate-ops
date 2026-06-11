package com.karate.tournament.web;

import com.karate.tournament.dto.request.MemberAccountCreateRequest;
import com.karate.tournament.dto.response.ClubManagerRoleResponse;
import com.karate.tournament.dto.response.MemberAccountCreateResponse;
import com.karate.tournament.service.MemberAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}")
@RequiredArgsConstructor
public class MemberAccountController {
  private final MemberAccountService memberAccounts;

  @PostMapping("/member-accounts")
  @ResponseStatus(HttpStatus.CREATED)
  public MemberAccountCreateResponse createMemberAccount(
      @PathVariable UUID organizationId,
      @Valid @RequestBody MemberAccountCreateRequest request
  ) {
    return memberAccounts.createMemberAccount(organizationId, request);
  }

  @PostMapping("/users/{userId}/club-manager-role")
  public ClubManagerRoleResponse assignClubManagerRole(
      @PathVariable UUID organizationId,
      @PathVariable UUID userId
  ) {
    return memberAccounts.assignClubManagerRole(organizationId, userId);
  }
}
