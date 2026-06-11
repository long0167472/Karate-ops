package com.karate.tournament.service.impl;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.MemberAccountCreateRequest;
import com.karate.tournament.dto.response.ClubManagerRoleResponse;
import com.karate.tournament.dto.response.MemberAccountCreateResponse;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.Role;
import com.karate.tournament.entity.UserRoleAssignment;
import com.karate.tournament.entity.enums.SystemRole;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.RoleRepository;
import com.karate.tournament.repository.UserRoleAssignmentRepository;
import com.karate.tournament.service.AccountNotificationService;
import com.karate.tournament.service.AccountProvisioningService;
import com.karate.tournament.service.MemberAccountService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberAccountServiceImpl implements MemberAccountService {
  private final OrganizationRepository organizations;
  private final AppUserRepository users;
  private final RoleRepository roles;
  private final UserRoleAssignmentRepository roleAssignments;
  private final PermissionService permissions;
  private final AccountProvisioningService provisioning;
  private final AccountNotificationService notifications;

  @Transactional
  public MemberAccountCreateResponse createMemberAccount(UUID organizationId, MemberAccountCreateRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organization.id);
    AccountProvisioningService.ProvisionedAccount provisioned = provisioning.createMemberAccount(organization, request);
    notifications.directAccountCreated(organization, provisioned.user(), provisioned.temporaryPassword());
    return new MemberAccountCreateResponse(provisioned.member(), provisioned.user().username, provisioned.temporaryPassword());
  }

  @Transactional
  public ClubManagerRoleResponse assignClubManagerRole(UUID organizationId, UUID userId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireGlobalAdmin();
    AppUser user = users.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    user.primaryOrganization = organization;
    assignRole(user, SystemRole.CLUB_MANAGER, "ORGANIZATION", organization.id);
    return new ClubManagerRoleResponse(user.id, user.username, organization.id, SystemRole.CLUB_MANAGER.name());
  }

  private void assignRole(AppUser user, SystemRole systemRole, String scopeType, UUID scopeId) {
    Role role = roles.findByCodeAndDeletedAtIsNull(systemRole.name())
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + systemRole.name()));
    roleAssignments.findByUser_IdAndRole_CodeAndScopeTypeAndScopeIdAndDeletedAtIsNull(user.id, role.code, scopeType, scopeId)
        .orElseGet(() -> {
          UserRoleAssignment assignment = UserRoleAssignment.create();
          assignment.user = user;
          assignment.role = role;
          assignment.scopeType = scopeType;
          assignment.scopeId = scopeId;
          return roleAssignments.save(assignment);
        });
  }

  private Organization requireOrganization(UUID organizationId) {
    return organizations.findByIdAndDeletedAtIsNull(organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
  }
}
