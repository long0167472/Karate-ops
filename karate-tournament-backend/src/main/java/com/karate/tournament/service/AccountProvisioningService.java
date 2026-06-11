package com.karate.tournament.service;

import com.karate.tournament.dto.request.MemberAccountCreateRequest;
import com.karate.tournament.dto.response.ClubMemberResponse;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;

public interface AccountProvisioningService {
  String DEFAULT_TEMPORARY_PASSWORD = "123456";

  ProvisionedAccount createMemberAccount(Organization organization, MemberAccountCreateRequest request);

  record ProvisionedAccount(
      ClubMemberResponse member,
      AppUser user,
      String temporaryPassword
  ) {
  }
}
