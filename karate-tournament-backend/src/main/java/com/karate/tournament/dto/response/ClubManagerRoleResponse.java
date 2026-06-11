package com.karate.tournament.dto.response;

import java.util.UUID;

public record ClubManagerRoleResponse(
    UUID userId,
    String username,
    UUID organizationId,
    String role
) {
}
