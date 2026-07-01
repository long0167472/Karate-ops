package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.ClubFeeItem;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.enums.BillingCycle;
import com.karate.tournament.entity.enums.FeeItemKind;
import com.karate.tournament.entity.enums.FeeItemStatus;
import com.karate.tournament.entity.enums.FeeItemType;
import com.karate.tournament.entity.enums.OrganizationType;
import com.karate.tournament.repository.ClubFeeItemRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.dto.response.ManagedClubResponse;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.OrganizationCreateRequest;
import com.karate.tournament.dto.response.OrganizationResponse;
import com.karate.tournament.dto.request.OrganizationUpdateRequest;
import com.karate.tournament.dto.response.OrganizationDashboardOverviewResponse;
import com.karate.tournament.entity.enums.SystemRole;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
  private final OrganizationRepository organizations;
  private final ClubFeeItemRepository feeItems;
  private final OrganizationDashboardService dashboards;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<OrganizationResponse> list() {
    permissions.requireGlobalAdmin();
    return organizations.findByDeletedAtIsNullOrderByNameAsc().stream().map(mapper::organization).toList();
  }

  @Transactional(readOnly = true)
  public List<ManagedClubResponse> managedClubs() {
    CurrentActor actor = permissions.currentActor();
    List<Organization> clubs;
    if (actor.hasRole(SystemRole.GLOBAL_ADMIN)) {
      clubs = organizations.findByTypeAndDeletedAtIsNullOrderByNameAsc(OrganizationType.CLUB);
    } else if (actor.primaryOrganizationId() != null) {
      clubs = organizations.findByIdAndDeletedAtIsNull(actor.primaryOrganizationId())
          .filter(organization -> organization.type == OrganizationType.CLUB && permissions.canViewClub(organization.id))
          .stream()
          .toList();
    } else {
      clubs = List.of();
    }
    Map<UUID, OrganizationDashboardOverviewResponse> overviewByClubId = dashboards.overviews(clubs);
    return clubs.stream()
        .map(club -> new ManagedClubResponse(mapper.organization(club), overviewByClubId.get(club.id)))
        .toList();
  }

  @Transactional(readOnly = true)
  public OrganizationResponse get(UUID id) {
    permissions.requireClubView(id);
    return mapper.organization(requireOrganization(id));
  }

  @Transactional
  public OrganizationResponse create(OrganizationCreateRequest request) {
    permissions.requireGlobalAdmin();
    Organization organization = Organization.create();
    organization.name = request.name();
    organization.shortName = request.shortName();
    organization.code = request.code();
    organization.type = request.type();
    organization.country = request.country();
    organization.province = request.province();
    organization.address = request.address();
    organization.contactEmail = request.contactEmail();
    organization.contactPhone = request.contactPhone();
    Organization saved = organizations.save(organization);
    if (saved.type == OrganizationType.CLUB) {
      createDefaultTuition(saved);
    }
    return mapper.organization(saved);
  }

  @Transactional
  public OrganizationResponse update(UUID id, OrganizationUpdateRequest request) {
    Organization organization = requireOrganization(id);
    permissions.requireRosterManage(organization.id);
    if (request.name() != null) organization.name = request.name();
    if (request.shortName() != null) organization.shortName = request.shortName();
    if (request.code() != null) organization.code = request.code();
    if (request.type() != null) organization.type = request.type();
    if (request.status() != null) organization.status = request.status();
    if (request.country() != null) organization.country = request.country();
    if (request.province() != null) organization.province = request.province();
    if (request.address() != null) organization.address = request.address();
    if (request.contactEmail() != null) organization.contactEmail = request.contactEmail();
    if (request.contactPhone() != null) organization.contactPhone = request.contactPhone();
    return mapper.organization(organization);
  }

  @Transactional
  public void delete(UUID id) {
    Organization organization = requireOrganization(id);
    permissions.requireGlobalAdmin();
    organization.softDelete();
  }

  public Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }

  private void createDefaultTuition(Organization organization) {
    ClubFeeItem item = ClubFeeItem.create();
    item.organization = organization;
    item.name = "Học phí";
    item.feeType = FeeItemType.TUITION;
    item.feeKind = FeeItemKind.MONTHLY_TUITION_DEFAULT;
    item.billingCycle = BillingCycle.MONTHLY;
    item.status = FeeItemStatus.ACTIVE;
    item.defaultAmount = java.math.BigDecimal.ZERO;
    item.dueDay = 10;
    item.description = "Khoản học phí tháng mặc định của CLB.";
    feeItems.save(item);
  }
}
