package com.karate.tournament.service;

import com.karate.tournament.entity.Person;
import com.karate.tournament.dto.request.PersonCreateRequest;
import com.karate.tournament.dto.response.PersonResponse;
import com.karate.tournament.dto.request.PersonUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface PersonService {
  List<PersonResponse> list();
  PersonResponse get(UUID id);
  PersonResponse create(PersonCreateRequest request);
  PersonResponse update(UUID id, PersonUpdateRequest request);
  PersonResponse updateInOrganization(UUID organizationId, UUID id, PersonUpdateRequest request);
  void delete(UUID id);
  Person requirePerson(UUID id);
}
