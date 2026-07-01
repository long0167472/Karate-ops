package com.karate.tournament.service;

import com.karate.tournament.dto.response.OrganizationAttendanceDashboardResponse;
import com.karate.tournament.dto.response.OrganizationAthleteDashboardResponse;
import com.karate.tournament.dto.response.OrganizationDashboardOverviewResponse;
import com.karate.tournament.entity.Organization;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrganizationDashboardService {
  OrganizationDashboardOverviewResponse overview(UUID organizationId);
  Map<UUID, OrganizationDashboardOverviewResponse> overviews(List<Organization> organizations);
  OrganizationAttendanceDashboardResponse attendance(UUID organizationId, LocalDate from, LocalDate to);
  OrganizationAthleteDashboardResponse athlete(UUID organizationId, UUID athleteId);
}
