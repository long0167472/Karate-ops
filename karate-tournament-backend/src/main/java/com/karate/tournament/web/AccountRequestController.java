package com.karate.tournament.web;

import com.karate.tournament.dto.request.AccountRequestCreateRequest;
import com.karate.tournament.dto.request.AccountRequestDecisionRequest;
import com.karate.tournament.dto.response.AccountRequestResponse;
import com.karate.tournament.dto.response.MemberAccountCreateResponse;
import com.karate.tournament.dto.response.PublicClubLookupResponse;
import com.karate.tournament.entity.enums.AccountRequestStatus;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.service.AccountRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountRequestController {
  private final AccountRequestService accountRequests;

  @GetMapping("/public/clubs/lookup")
  public PublicClubLookupResponse lookupClub(@RequestParam String code) {
    return accountRequests.lookupClub(code);
  }

  @PostMapping("/account-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountRequestResponse create(@Valid @RequestBody AccountRequestCreateRequest request) {
    return accountRequests.create(request);
  }

  @GetMapping("/organizations/{organizationId}/account-requests")
  public List<AccountRequestResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(defaultValue = "ALL") String status
  ) {
    return accountRequests.list(organizationId, parseStatus(status));
  }

  @PatchMapping("/organizations/{organizationId}/account-requests/{requestId}/decision")
  public MemberAccountCreateResponse decide(
      @PathVariable UUID organizationId,
      @PathVariable UUID requestId,
      @Valid @RequestBody AccountRequestDecisionRequest request
  ) {
    return accountRequests.decide(organizationId, requestId, request);
  }

  private AccountRequestStatus parseStatus(String value) {
    if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
      return null;
    }
    try {
      return AccountRequestStatus.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException("Unsupported account request status: " + value);
    }
  }
}
