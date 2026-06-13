package com.karate.tournament.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record BeltExamCandidateRequest(
    UUID organizationMemberId,
    UUID athleteId,
    String currentBelt,
    @NotBlank String targetBelt
) {
}
