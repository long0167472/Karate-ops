package com.karate.tournament.service.impl;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.AccountRequestCreateRequest;
import com.karate.tournament.dto.request.AccountRequestDecisionRequest;
import com.karate.tournament.dto.request.MemberAccountCreateRequest;
import com.karate.tournament.dto.response.AccountRequestResponse;
import com.karate.tournament.dto.response.MemberAccountCreateResponse;
import com.karate.tournament.dto.response.PublicClubLookupResponse;
import com.karate.tournament.entity.AccountRequest;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.enums.AccountRequestStatus;
import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.OrganizationStatus;
import com.karate.tournament.entity.enums.OrganizationType;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AccountRequestRepository;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.service.AccountNotificationService;
import com.karate.tournament.service.AccountProvisioningService;
import com.karate.tournament.service.AccountRequestService;
import com.karate.tournament.web.ApiMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountRequestServiceImpl implements AccountRequestService {
  private final AccountRequestRepository accountRequests;
  private final OrganizationRepository organizations;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final AccountProvisioningService provisioning;
  private final AccountNotificationService notifications;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public PublicClubLookupResponse lookupClub(String code) {
    return mapper.publicClub(requireActiveClubByCode(code));
  }

  @Transactional
  public AccountRequestResponse create(AccountRequestCreateRequest request) {
    Organization organization = requireActiveClubByCode(request.organizationCode());
    String email = normalizeEmail(request.email());
    users.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(existing -> {
      throw new BusinessConflictException("Email is already registered");
    });
    accountRequests
        .findByOrganization_IdAndEmailIgnoreCaseAndStatusAndDeletedAtIsNull(organization.id, email, AccountRequestStatus.PENDING)
        .ifPresent(existing -> {
          throw new BusinessConflictException("This email already has a pending request for this club");
        });

    AccountRequest accountRequest = AccountRequest.create();
    accountRequest.organization = organization;
    accountRequest.displayName = cleanRequired(request.displayName(), "displayName");
    accountRequest.email = email;
    accountRequest.phone = cleanRequired(request.phone(), "phone");
    accountRequest.gender = request.gender();
    accountRequest.birthDate = request.birthDate();
    accountRequest.currentAddress = trimToNull(request.currentAddress());
    accountRequest.status = AccountRequestStatus.PENDING;
    return mapper.accountRequest(accountRequests.save(accountRequest));
  }

  @Transactional(readOnly = true)
  public List<AccountRequestResponse> list(UUID organizationId, AccountRequestStatus status) {
    permissions.requireRosterManage(organizationId);
    List<AccountRequest> rows = status == null
        ? accountRequests.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        : accountRequests.findByOrganization_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId, status);
    return rows.stream().map(mapper::accountRequest).toList();
  }

  @Transactional
  public MemberAccountCreateResponse decide(UUID organizationId, UUID requestId, AccountRequestDecisionRequest request) {
    if (request.status() == AccountRequestStatus.PENDING) {
      throw new BadRequestException("Decision status must be APPROVED or REJECTED");
    }
    // Pessimistic lock: concurrent decisions serialize so the account cannot be provisioned twice.
    AccountRequest accountRequest = accountRequests.findWithLockById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Account request not found: " + requestId));
    if (!accountRequest.organization.id.equals(organizationId)) {
      throw new ResourceNotFoundException("Account request does not belong to organization");
    }
    permissions.requireRosterManage(organizationId);
    if (accountRequest.status != AccountRequestStatus.PENDING) {
      throw new BusinessConflictException("Account request was already decided");
    }
    if (request.status() == AccountRequestStatus.REJECTED && trimToNull(request.decisionNote()) == null) {
      throw new BadRequestException("Reject reason is required");
    }

    accountRequest.status = request.status();
    accountRequest.decisionNote = trimToNull(request.decisionNote());
    accountRequest.decidedAt = Instant.now();
    accountRequest.decidedByUser = currentUser();
    if (request.status() == AccountRequestStatus.REJECTED) {
      notifications.accountRejected(accountRequest);
      return new MemberAccountCreateResponse(null, null, null);
    }

    AccountProvisioningService.ProvisionedAccount provisioned = provisioning.createMemberAccount(
        accountRequest.organization,
        new MemberAccountCreateRequest(
            accountRequest.displayName,
            accountRequest.email,
            accountRequest.phone,
            accountRequest.gender,
            accountRequest.birthDate,
            accountRequest.currentAddress,
            null,
            null,
            ClubMemberRole.ATHLETE,
            ClubMemberStatus.ACTIVE,
            true,
            true,
            null,
            null
        )
    );
    accountRequest.approvedUser = provisioned.user();
    notifications.accountApproved(accountRequest, provisioned.user().username, provisioned.temporaryPassword());
    return new MemberAccountCreateResponse(provisioned.member(), provisioned.user().username, provisioned.temporaryPassword());
  }

  private Organization requireActiveClubByCode(String code) {
    String normalizedCode = cleanRequired(code, "organizationCode");
    Organization organization = organizations.findByCodeIgnoreCaseAndDeletedAtIsNull(normalizedCode)
        .orElseThrow(() -> new ResourceNotFoundException("Club not found: " + normalizedCode));
    if (organization.type != OrganizationType.CLUB || organization.status != OrganizationStatus.ACTIVE) {
      throw new ResourceNotFoundException("Active club not found: " + normalizedCode);
    }
    return organization;
  }

  private AppUser currentUser() {
    UUID userId = permissions.currentActor().userId();
    return users.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
  }

  private String normalizeEmail(String email) {
    return cleanRequired(email, "email").toLowerCase();
  }

  private String cleanRequired(String value, String field) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      throw new BadRequestException(field + " is required");
    }
    return trimmed;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
