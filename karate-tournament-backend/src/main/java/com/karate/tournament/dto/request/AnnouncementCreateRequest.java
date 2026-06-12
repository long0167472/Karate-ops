package com.karate.tournament.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementCreateRequest(
    @NotBlank @Size(max = 180) String title,
    @NotBlank @Size(max = 4000) String content,
    Boolean pinned
) {
}
