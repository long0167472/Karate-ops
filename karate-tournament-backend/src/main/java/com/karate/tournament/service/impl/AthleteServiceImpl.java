package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ForbiddenException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.enums.AthleteStatus;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.Person;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.ClubRosterRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.PersonRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.AthleteCreateRequest;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.dto.request.AthleteUpdateRequest;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AthleteServiceImpl implements AthleteService {
  private final AthleteRepository athletes;
  private final PersonRepository persons;
  private final OrganizationRepository organizations;
  private final OrganizationMemberRepository members;
  private final ClubRosterRepository roster;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<AthleteResponse> list() {
    permissions.requireGlobalAdmin();
    return athletes.findByDeletedAtIsNullOrderByCreatedAtDesc().stream().map(mapper::athlete).toList();
  }

  @Transactional(readOnly = true)
  public List<AthleteResponse> listByOrganization(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    return athletes.findVisibleInOrganization(organization.id).stream().map(mapper::athlete).toList();
  }

  @Transactional(readOnly = true)
  public AthleteResponse get(UUID id) {
    Athlete athlete = requireAthlete(id);
    if (athlete.primaryOrganization != null) {
      permissions.requireClubView(athlete.primaryOrganization.id);
    } else {
      permissions.requireGlobalAdmin();
    }
    return mapper.athlete(athlete);
  }

  @Transactional
  public AthleteResponse create(AthleteCreateRequest request) {
    Person person = persons.findByIdAndDeletedAtIsNull(request.personId())
        .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + request.personId()));
    athletes.findByPerson_IdAndDeletedAtIsNull(person.id).ifPresent(existing -> {
      throw new BusinessConflictException("Person is already linked to an athlete");
    });
    Organization organization = null;
    if (request.primaryOrganizationId() != null) {
      organization = requireOrganization(request.primaryOrganizationId());
      permissions.requireRosterManage(organization.id);
    } else {
      permissions.requireGlobalAdmin();
    }
    Athlete athlete = Athlete.create();
    athlete.person = person;
    athlete.primaryOrganization = organization;
    athlete.externalCode = request.externalCode();
    athlete.belt = request.belt();
    athlete.weightKg = request.weightKg();
    athlete.heightCm = request.heightCm();
    athlete.status = request.status() == null ? AthleteStatus.ACTIVE : request.status();
    return mapper.athlete(athletes.save(athlete));
  }

  @Transactional
  public AthleteResponse createInOrganization(UUID organizationId, AthleteCreateRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireRosterManage(organization.id);
    Person person = persons.findByIdAndDeletedAtIsNull(request.personId())
        .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + request.personId()));
    members.findByOrganization_IdAndPerson_IdAndStatusAndDeletedAtIsNull(organization.id, person.id, ClubMemberStatus.ACTIVE)
        .orElseThrow(() -> new BusinessConflictException("Athlete person must be an ACTIVE club member before creating an athlete profile"));
    athletes.findByPerson_IdAndDeletedAtIsNull(person.id).ifPresent(existing -> {
      throw new BusinessConflictException("Person is already linked to an athlete");
    });
    Athlete athlete = Athlete.create();
    athlete.person = person;
    athlete.primaryOrganization = organization;
    athlete.externalCode = request.externalCode();
    athlete.belt = request.belt();
    athlete.weightKg = request.weightKg();
    athlete.heightCm = request.heightCm();
    athlete.status = request.status() == null ? AthleteStatus.ACTIVE : request.status();
    return mapper.athlete(athletes.save(athlete));
  }

  @Transactional
  public AthleteResponse update(UUID id, AthleteUpdateRequest request) {
    Athlete athlete = requireAthlete(id);
    if (athlete.primaryOrganization != null) {
      permissions.requireRosterManage(athlete.primaryOrganization.id);
    } else {
      permissions.requireGlobalAdmin();
    }
    applyUpdate(athlete, request);
    return mapper.athlete(athlete);
  }

  @Transactional
  public AthleteResponse updateInOrganization(UUID organizationId, UUID id, AthleteUpdateRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    Athlete athlete = requireAthlete(id);
    if (!isVisibleInOrganization(organization.id, athlete)) {
      throw new ResourceNotFoundException("Athlete is not visible in this organization");
    }
    requirePrimaryClubEditPermission(organization.id, athlete);
    applyUpdate(athlete, request);
    return mapper.athlete(athlete);
  }

  @Transactional
  public void delete(UUID id) {
    Athlete athlete = requireAthlete(id);
    if (athlete.primaryOrganization != null) {
      permissions.requireRosterManage(athlete.primaryOrganization.id);
    } else {
      permissions.requireGlobalAdmin();
    }
    athlete.softDelete();
  }

  public Athlete requireAthlete(UUID id) {
    return athletes.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + id));
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }

  private void applyUpdate(Athlete athlete, AthleteUpdateRequest request) {
    if (request.primaryOrganizationId() != null) {
      Organization targetOrganization = requireOrganization(request.primaryOrganizationId());
      boolean primaryChanged = athlete.primaryOrganization == null || !athlete.primaryOrganization.id.equals(targetOrganization.id);
      if (primaryChanged && !hasActiveRosterInOrganization(athlete.id, targetOrganization.id)) {
        throw new BusinessConflictException("Primary organization must already have an ACTIVE roster for this athlete");
      }
      athlete.primaryOrganization = targetOrganization;
    }
    if (request.externalCode() != null) athlete.externalCode = request.externalCode();
    if (request.belt() != null) athlete.belt = request.belt();
    if (request.weightKg() != null) athlete.weightKg = request.weightKg();
    if (request.heightCm() != null) athlete.heightCm = request.heightCm();
    if (request.status() != null) athlete.status = request.status();
  }

  private void requirePrimaryClubEditPermission(UUID organizationId, Athlete athlete) {
    if (athlete.primaryOrganization == null || !athlete.primaryOrganization.id.equals(organizationId)) {
      throw new ForbiddenException("Only the primary club can edit this athlete profile");
    }
    permissions.requireRosterManage(organizationId);
  }

  private boolean isVisibleInOrganization(UUID organizationId, Athlete athlete) {
    return athlete.primaryOrganization != null && athlete.primaryOrganization.id.equals(organizationId)
        || hasActiveRosterInOrganization(athlete.id, organizationId);
  }

  private boolean hasActiveRosterInOrganization(UUID athleteId, UUID organizationId) {
    return roster.findByOrganization_IdAndAthlete_IdAndStatusAndDeletedAtIsNull(
        organizationId,
        athleteId,
        ClubRosterStatus.ACTIVE
    ).isPresent();
  }
}
