package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Person;
import com.karate.tournament.repository.PersonRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.PersonCreateRequest;
import com.karate.tournament.dto.response.PersonResponse;
import com.karate.tournament.dto.request.PersonUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {
  private final PersonRepository persons;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<PersonResponse> list() {
    return persons.findByDeletedAtIsNullOrderByDisplayNameAsc().stream().map(mapper::person).toList();
  }

  @Transactional(readOnly = true)
  public PersonResponse get(UUID id) {
    return mapper.person(requirePerson(id));
  }

  @Transactional
  public PersonResponse create(PersonCreateRequest request) {
    permissions.currentActor();
    Person person = Person.create();
    person.displayName = request.displayName();
    person.firstName = request.firstName();
    person.lastName = request.lastName();
    person.birthDate = request.birthDate();
    person.gender = request.gender();
    person.nationalId = request.nationalId();
    person.email = request.email();
    person.phone = request.phone();
    person.currentAddress = request.currentAddress();
    person.emergencyContactName = request.emergencyContactName();
    person.emergencyContactPhone = request.emergencyContactPhone();
    return mapper.person(persons.save(person));
  }

  @Transactional
  public PersonResponse update(UUID id, PersonUpdateRequest request) {
    permissions.requireGlobalAdmin();
    Person person = requirePerson(id);
    if (request.displayName() != null) person.displayName = request.displayName();
    if (request.firstName() != null) person.firstName = request.firstName();
    if (request.lastName() != null) person.lastName = request.lastName();
    if (request.birthDate() != null) person.birthDate = request.birthDate();
    if (request.gender() != null) person.gender = request.gender();
    if (request.nationalId() != null) person.nationalId = request.nationalId();
    if (request.email() != null) person.email = request.email();
    if (request.phone() != null) person.phone = request.phone();
    if (request.currentAddress() != null) person.currentAddress = request.currentAddress();
    if (request.emergencyContactName() != null) person.emergencyContactName = request.emergencyContactName();
    if (request.emergencyContactPhone() != null) person.emergencyContactPhone = request.emergencyContactPhone();
    return mapper.person(person);
  }

  @Transactional
  public void delete(UUID id) {
    permissions.requireGlobalAdmin();
    requirePerson(id).softDelete();
  }

  public Person requirePerson(UUID id) {
    return persons.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
  }
}
