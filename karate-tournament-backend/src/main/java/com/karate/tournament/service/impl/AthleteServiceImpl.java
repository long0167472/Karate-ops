package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.enums.AthleteStatus;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.Person;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.PersonRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.AthleteCreateRequest;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.dto.request.AthleteUpdateRequest;
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
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<AthleteResponse> list() {
    return athletes.findByDeletedAtIsNullOrderByCreatedAtDesc().stream().map(mapper::athlete).toList();
  }

  @Transactional(readOnly = true)
  public AthleteResponse get(UUID id) {
    return mapper.athlete(requireAthlete(id));
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
  public AthleteResponse update(UUID id, AthleteUpdateRequest request) {
    Athlete athlete = requireAthlete(id);
    if (athlete.primaryOrganization != null) {
      permissions.requireRosterManage(athlete.primaryOrganization.id);
    } else {
      permissions.requireGlobalAdmin();
    }
    if (request.primaryOrganizationId() != null) athlete.primaryOrganization = requireOrganization(request.primaryOrganizationId());
    if (request.externalCode() != null) athlete.externalCode = request.externalCode();
    if (request.belt() != null) athlete.belt = request.belt();
    if (request.weightKg() != null) athlete.weightKg = request.weightKg();
    if (request.heightCm() != null) athlete.heightCm = request.heightCm();
    if (request.status() != null) athlete.status = request.status();
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
}
