package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.ClubRoster;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.ClubRosterRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.ClubRosterCreateRequest;
import com.karate.tournament.dto.response.ClubRosterResponse;
import com.karate.tournament.dto.request.ClubRosterUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubRosterServiceImpl implements ClubRosterService {
  private final ClubRosterRepository roster;
  private final OrganizationRepository organizations;
  private final AthleteRepository athletes;
  private final OrganizationMemberRepository members;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<ClubRosterResponse> list(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    return roster.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::clubRoster)
        .toList();
  }

  @Transactional
  public ClubRosterResponse create(UUID organizationId, ClubRosterCreateRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organization.id);
    Athlete athlete = athletes.findByIdAndDeletedAtIsNull(request.athleteId())
        .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + request.athleteId()));
    validateAthleteCanJoinRoster(organization, athlete);
    roster.findByOrganization_IdAndAthlete_IdAndDeletedAtIsNull(organization.id, athlete.id)
        .ifPresent(existing -> {
          throw new BusinessConflictException("Athlete is already in this club roster");
        });
    if (athlete.primaryOrganization == null) {
      athlete.primaryOrganization = organization;
    }
    ClubRoster row = ClubRoster.create();
    row.organization = organization;
    row.athlete = athlete;
    row.status = request.status() == null ? ClubRosterStatus.ACTIVE : request.status();
    row.joinedAt = request.joinedAt() == null ? LocalDate.now() : request.joinedAt();
    return mapper.clubRoster(roster.save(row));
  }

  @Transactional
  public ClubRosterResponse update(UUID organizationId, UUID rosterId, ClubRosterUpdateRequest request) {
    ClubRoster row = requireRosterInOrganization(organizationId, rosterId);
    permissions.requireRosterManage(organizationId);
    if (request.status() != null) row.status = request.status();
    if (request.joinedAt() != null) row.joinedAt = request.joinedAt();
    return mapper.clubRoster(row);
  }

  @Transactional
  public void delete(UUID organizationId, UUID rosterId) {
    ClubRoster row = requireRosterInOrganization(organizationId, rosterId);
    permissions.requireRosterManage(organizationId);
    row.softDelete();
  }

  public void requireAthleteBelongsToOrganization(UUID organizationId, Athlete athlete) {
    if (athlete.primaryOrganization != null && athlete.primaryOrganization.id.equals(organizationId)) {
      return;
    }
    roster.findByOrganization_IdAndAthlete_IdAndStatusAndDeletedAtIsNull(organizationId, athlete.id, ClubRosterStatus.ACTIVE)
        .orElseThrow(() -> new BusinessConflictException("Athlete is not active in this club roster"));
  }

  private ClubRoster requireRosterInOrganization(UUID organizationId, UUID rosterId) {
    ClubRoster row = roster.findByIdAndDeletedAtIsNull(rosterId)
        .orElseThrow(() -> new ResourceNotFoundException("Club roster row not found: " + rosterId));
    if (!row.organization.id.equals(organizationId)) {
      throw new ResourceNotFoundException("Roster row does not belong to organization");
    }
    return row;
  }

  private void validateAthleteCanJoinRoster(Organization organization, Athlete athlete) {
    if (athlete.primaryOrganization != null && !athlete.primaryOrganization.id.equals(organization.id)) {
      throw new BusinessConflictException("Athlete primary organization is different from this club");
    }
    members.findByOrganization_IdAndPerson_IdAndStatusAndDeletedAtIsNull(
        organization.id,
        athlete.person.id,
        ClubMemberStatus.ACTIVE
    ).orElseThrow(() -> new BusinessConflictException("Athlete person must be an ACTIVE club member before joining roster"));
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }
}
