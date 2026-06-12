package com.karate.tournament.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PublicTournamentSummaryResponse(
    UUID id,
    String name,
    String organizerName,
    String location,
    LocalDate startsOn,
    LocalDate endsOn,
    String status,
    int participantCount,
    boolean phongTraoEnabled,
    boolean nangCaoEnabled,
    boolean registrationOpen,
    Instant registrationDeadline
) {
}
