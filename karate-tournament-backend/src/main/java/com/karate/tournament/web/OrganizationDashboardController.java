package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.OrganizationDashboardService;
import com.karate.tournament.dto.response.OrganizationAttendanceDashboardResponse;
import com.karate.tournament.dto.response.OrganizationAthleteDashboardResponse;
import com.karate.tournament.dto.response.OrganizationDashboardOverviewResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/organizations/{organizationId}")
@RequiredArgsConstructor
public class OrganizationDashboardController {
  private final OrganizationDashboardService dashboard;

  @GetMapping("/overview")
  public OrganizationDashboardOverviewResponse overview(@PathVariable UUID organizationId) {
    return dashboard.overview(organizationId);
  }

  @GetMapping("/attendance")
  public OrganizationAttendanceDashboardResponse attendance(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    return dashboard.attendance(organizationId, from, to);
  }

  @GetMapping("/athletes/{athleteId}")
  public OrganizationAthleteDashboardResponse athlete(
      @PathVariable UUID organizationId,
      @PathVariable UUID athleteId
  ) {
    return dashboard.athlete(organizationId, athleteId);
  }
}
