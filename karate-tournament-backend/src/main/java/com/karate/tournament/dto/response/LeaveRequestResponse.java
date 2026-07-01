package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.LeaveRequestStatus;
import com.karate.tournament.entity.enums.LeaveRequestType;
import java.time.Instant;
import java.time.LocalDate;
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
    LeaveRequestType requestType,
    LeaveRequestStatus status,
    String reason,
    LocalDate fromDate,
    LocalDate toDate,
    String decisionNote,
    Instant decidedAt,
    Instant createdAt,
    Instant expiresAt
) {
}
