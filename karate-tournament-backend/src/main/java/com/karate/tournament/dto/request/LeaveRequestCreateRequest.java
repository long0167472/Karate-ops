package com.karate.tournament.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LeaveRequestCreateRequest(
    @NotNull UUID sessionId,
    @NotBlank String reason
) {
}
