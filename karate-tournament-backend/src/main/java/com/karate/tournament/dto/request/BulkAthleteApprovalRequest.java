package com.karate.tournament.dto.request;

import java.util.List;
import java.util.UUID;

public record BulkAthleteApprovalRequest(
    List<UUID> entryIds,
    UUID participantId,
    Boolean approveAll
) {
}
