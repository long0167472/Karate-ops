package com.karate.tournament.dto.request;

import jakarta.validation.constraints.Size;

public record AnnouncementUpdateRequest(
    @Size(max = 180) String title,
    @Size(max = 4000) String content,
    Boolean pinned
) {
}
