package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.LeaveRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record LeaveRequestResponse(
    UUID id,
    UUID sessionId,
    String sessionName,
    UUID organizationId,
    String organizationName,
    UUID memberId,
    String memberName,
    UUID requesterUserId,
    UUID decidedByUserId,
    LeaveRequestStatus status,
    String reason,
    String decisionNote,
    Instant decidedAt,
    Instant createdAt
) {
}
