package com.karate.tournament.service.impl;

import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.exception.UnauthorizedException;
import com.karate.tournament.auth.AuthenticatedPrincipal;
import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.JwtService;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.enums.OrganizationStatus;
import com.karate.tournament.entity.enums.OrganizationType;
import com.karate.tournament.entity.Role;
import com.karate.tournament.entity.enums.SystemRole;
import com.karate.tournament.entity.UserRoleAssignment;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.RoleRepository;
import com.karate.tournament.repository.UserRoleAssignmentRepository;
import com.karate.tournament.dto.response.AuthResponse;
import com.karate.tournament.dto.response.AuthUserResponse;
import com.karate.tournament.dto.request.LoginRequest;
import com.karate.tournament.dto.request.RegisterClubManagerRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService, ApplicationRunner {
  private final AppUserRepository users;
  private final OrganizationRepository organizations;
  private final RoleRepository roles;
  private final UserRoleAssignmentRepository roleAssignments;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  @Value("${app.security.seed-admin.email}")
  private final String seedEmail;
  @Value("${app.security.seed-admin.password}")
  private final String seedPassword;
  @Value("${app.security.seed-admin.display-name}")
  private final String seedDisplayName;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    ensureRoles();
    seedAdmin();
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    AppUser user = users.findByEmailOrUsername(normalizeCredential(request.email()))
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    if (!"ACTIVE".equals(user.status) || user.passwordHash == null || !passwordEncoder.matches(request.password(), user.passwordHash)) {
      throw new UnauthorizedException("Invalid email or password");
    }
    user.lastLoginAt = Instant.now();
    return authResponse(user);
  }

  @Transactional
  public AuthResponse registerClubManager(RegisterClubManagerRequest request) {
    throw new BadRequestException("Club manager registration is temporarily disabled");
  }

  @Transactional(readOnly = true)
  public AuthUserResponse me(CurrentActor actor) {
    AppUser user = users.findByIdAndDeletedAtIsNull(actor.userId())
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.userId()));
    return userResponse(user, actor.roles());
  }

  private AuthResponse authResponse(AppUser user) {
    Set<SystemRole> userRoles = roleAssignments.findByUser_IdAndDeletedAtIsNull(user.id).stream()
        .map(assignment -> SystemRole.valueOf(assignment.role.code))
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    userRoles.add(SystemRole.MEMBER);
    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
        user.id,
        user.primaryOrganization == null ? null : user.primaryOrganization.id,
        user.email,
        user.displayName,
        userRoles
    );
    return new AuthResponse(jwtService.createToken(principal), "Bearer", jwtService.expiresSeconds(), userResponse(user, userRoles));
  }

  private AuthUserResponse userResponse(AppUser user, Set<SystemRole> userRoles) {
    java.util.LinkedHashSet<SystemRole> effectiveRoles = new java.util.LinkedHashSet<>(userRoles);
    effectiveRoles.add(SystemRole.MEMBER);
    return new AuthUserResponse(
        user.id,
        user.displayName,
        user.email,
        user.username,
        user.phone,
        user.primaryOrganization == null ? null : user.primaryOrganization.id,
        user.primaryOrganization == null ? null : user.primaryOrganization.name,
        user.status,
        effectiveRoles.stream().map(Enum::name).sorted().toList()
    );
  }

  private void seedAdmin() {
    validatePassword(seedPassword);
    String adminEmail = normalizeEmail(seedEmail);
    Organization adminOrg = organizations.findByIdAndDeletedAtIsNull(UUID.fromString("00000000-0000-0000-0000-000000000201"))
        .orElseGet(() -> {
          Organization organization = Organization.create();
          organization.id = UUID.fromString("00000000-0000-0000-0000-000000000201");
          organization.name = "Karate Ops Admin";
          organization.shortName = "Karate Ops";
          organization.code = "KARATE_OPS_ADMIN";
          organization.type = OrganizationType.ORGANIZER;
          organization.status = OrganizationStatus.ACTIVE;
          return organizations.save(organization);
        });
    AppUser admin = users.findByEmailIgnoreCaseAndDeletedAtIsNull(adminEmail).orElseGet(AppUser::create);
    if (admin.id == null) admin.id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    admin.displayName = seedDisplayName;
    admin.email = adminEmail;
    admin.username = admin.username == null || admin.username.isBlank() ? "global.admin" : admin.username;
    admin.status = "ACTIVE";
    admin.primaryOrganization = adminOrg;
    admin.passwordHash = passwordEncoder.encode(seedPassword);
    users.save(admin);
    assignRole(admin, SystemRole.GLOBAL_ADMIN, "GLOBAL", null);
  }

  private void ensureRoles() {
    Arrays.stream(SystemRole.values()).forEach(role -> roles.findByCodeAndDeletedAtIsNull(role.name()).orElseGet(() -> {
      Role row = Role.create();
      row.code = role.name();
      row.name = role.name().replace('_', ' ');
      row.description = "System role " + role.name();
      return roles.save(row);
    }));
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

  private void validatePassword(String password) {
    if (password == null || password.length() < 8) {
      throw new BadRequestException("Password must have at least 8 characters");
    }
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }

  private String normalizeCredential(String credential) {
    return credential == null ? "" : credential.trim().toLowerCase();
  }
}
