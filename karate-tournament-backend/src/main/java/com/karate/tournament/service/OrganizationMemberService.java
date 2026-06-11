package com.karate.tournament.service;

import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.dto.request.ClubMemberCreateRequest;
import com.karate.tournament.dto.response.ClubMemberResponse;
import com.karate.tournament.dto.request.ClubMemberUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface OrganizationMemberService {
  List<ClubMemberResponse> list(UUID organizationId);
  ClubMemberResponse create(UUID organizationId, ClubMemberCreateRequest request);
  ClubMemberResponse update(UUID organizationId, UUID memberId, ClubMemberUpdateRequest request);
  void delete(UUID organizationId, UUID memberId);
  OrganizationMember requireMemberInOrganization(UUID organizationId, UUID memberId);
}
