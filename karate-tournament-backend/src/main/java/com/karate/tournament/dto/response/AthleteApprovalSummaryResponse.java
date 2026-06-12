package com.karate.tournament.dto.response;

public record AthleteApprovalSummaryResponse(
    int totalEntries,
    int approved,
    int rejected,
    int pending
) {
}
