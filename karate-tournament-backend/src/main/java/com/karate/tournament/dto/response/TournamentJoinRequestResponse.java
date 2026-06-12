package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.TournamentJoinRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TournamentJoinRequestResponse(
    UUID id,
    UUID tournamentId,
    String tournamentName,
    LocalDate tournamentStartsOn,
    String tournamentStatus,
    UUID organizationId,
    String organizationName,
    UUID memberId,
    String memberName,
    UUID requesterUserId,
    UUID decidedByUserId,
    TournamentJoinRequestStatus status,
    String note,
    String decisionNote,
    Instant decidedAt,
    Instant createdAt
) {
}
