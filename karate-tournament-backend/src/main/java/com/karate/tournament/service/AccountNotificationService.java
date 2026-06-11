package com.karate.tournament.service;

import com.karate.tournament.entity.AccountRequest;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;

public interface AccountNotificationService {
  void accountApproved(AccountRequest request, String username, String temporaryPassword);

  void accountRejected(AccountRequest request);

  void directAccountCreated(Organization organization, AppUser user, String temporaryPassword);
}
