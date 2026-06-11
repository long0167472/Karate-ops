package com.karate.tournament.service;

import com.karate.tournament.dto.request.MemberAccountCreateRequest;
import com.karate.tournament.dto.response.ClubManagerRoleResponse;
import com.karate.tournament.dto.response.MemberAccountCreateResponse;
import java.util.UUID;

public interface MemberAccountService {
  MemberAccountCreateResponse createMemberAccount(UUID organizationId, MemberAccountCreateRequest request);

  ClubManagerRoleResponse assignClubManagerRole(UUID organizationId, UUID userId);
}
