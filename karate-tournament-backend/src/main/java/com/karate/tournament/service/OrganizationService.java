package com.karate.tournament.service;

import com.karate.tournament.entity.Organization;
import com.karate.tournament.dto.request.OrganizationCreateRequest;
import com.karate.tournament.dto.response.ManagedClubResponse;
import com.karate.tournament.dto.response.OrganizationResponse;
import com.karate.tournament.dto.request.OrganizationUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface OrganizationService {
  List<OrganizationResponse> list();
  List<ManagedClubResponse> managedClubs();
  OrganizationResponse get(UUID id);
  OrganizationResponse create(OrganizationCreateRequest request);
  OrganizationResponse update(UUID id, OrganizationUpdateRequest request);
  void delete(UUID id);
  Organization requireOrganization(UUID id);
}
