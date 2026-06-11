package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.ParticipantStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record TournamentParticipantResponse(
    UUID id,
    UUID tournamentId,
    UUID organizationId,
    String organizationName,
    String displayName,
    ParticipantStatus status,
    Instant approvedAt
) {
}
