package com.karate.tournament.dto.response;

import java.util.UUID;

public record ParticipantApprovalItemResponse(
    UUID participantId,
    UUID organizationId,
    String organizationName,
    String displayName,
    String status,
    int approvedEntries,
    int totalEntries
) {}
