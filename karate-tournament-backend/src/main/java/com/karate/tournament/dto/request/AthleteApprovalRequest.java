package com.karate.tournament.dto.request;

public record AthleteApprovalRequest(
    String action,
    String reason
) {
}
