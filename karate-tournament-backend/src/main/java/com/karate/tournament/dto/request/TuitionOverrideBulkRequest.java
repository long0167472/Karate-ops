package com.karate.tournament.dto.request;

import java.util.List;
import java.util.UUID;

public record TuitionOverrideBulkRequest(
    List<UUID> memberIds,
    UUID feeItemId
) {
}
