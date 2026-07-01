package com.karate.tournament.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ClubAnnouncementResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    String title,
    String content,
    boolean pinned,
    UUID createdByUserId,
    String createdByName,
    Instant createdAt,
    Instant updatedAt
) {
}
