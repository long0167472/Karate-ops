package com.karate.tournament.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TournamentJoinRequestCreateRequest(
    @NotNull UUID tournamentId,
    UUID organizationId,
    @Size(max = 500) String note
) {
}
