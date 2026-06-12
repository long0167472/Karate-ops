package com.karate.tournament.dto.response;

import java.util.UUID;

public record ParticipantApprovalResponse(
    UUID participantId,
    String organizationName,
    String status,
    int approvedEntries,
    int totalEntries
) {
}
