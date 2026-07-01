package com.karate.tournament.dto.response;

public record ManagedClubResponse(
    OrganizationResponse club,
    OrganizationDashboardOverviewResponse overview
) {
}
