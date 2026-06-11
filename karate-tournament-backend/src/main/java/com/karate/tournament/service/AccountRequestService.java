package com.karate.tournament.service;

import com.karate.tournament.dto.request.AccountRequestCreateRequest;
import com.karate.tournament.dto.request.AccountRequestDecisionRequest;
import com.karate.tournament.dto.response.AccountRequestResponse;
import com.karate.tournament.dto.response.MemberAccountCreateResponse;
import com.karate.tournament.dto.response.PublicClubLookupResponse;
import com.karate.tournament.entity.enums.AccountRequestStatus;
import java.util.List;
import java.util.UUID;

public interface AccountRequestService {
  PublicClubLookupResponse lookupClub(String code);

  AccountRequestResponse create(AccountRequestCreateRequest request);

  List<AccountRequestResponse> list(UUID organizationId, AccountRequestStatus status);

  MemberAccountCreateResponse decide(UUID organizationId, UUID requestId, AccountRequestDecisionRequest request);
}
