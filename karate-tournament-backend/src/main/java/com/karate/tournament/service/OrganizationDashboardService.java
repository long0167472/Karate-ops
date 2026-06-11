package com.karate.tournament.service;

import com.karate.tournament.dto.response.OrganizationAttendanceDashboardResponse;
import com.karate.tournament.dto.response.OrganizationAthleteDashboardResponse;
import com.karate.tournament.dto.response.OrganizationDashboardOverviewResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface OrganizationDashboardService {
  OrganizationDashboardOverviewResponse overview(UUID organizationId);
  OrganizationAttendanceDashboardResponse attendance(UUID organizationId, LocalDate from, LocalDate to);
  OrganizationAthleteDashboardResponse athlete(UUID organizationId, UUID athleteId);
}
