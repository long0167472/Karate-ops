package com.karate.tournament.dto.request;

public record ParticipantApprovalRequest(
    String action,
    String reason
) {
}
