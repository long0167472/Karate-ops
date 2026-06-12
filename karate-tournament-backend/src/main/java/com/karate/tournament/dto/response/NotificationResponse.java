package com.karate.tournament.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String type,
    String title,
    String body,
    String link,
    boolean read,
    Instant createdAt
) {
}
