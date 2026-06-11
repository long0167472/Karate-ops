package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.EntryStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record EntryCreateRequest(
    @NotNull UUID tournamentParticipantId,
    UUID athleteId,
    UUID teamId,
    Integer seedNo,
    EntryStatus status,
    BigDecimal registrationWeightKg,
    String teamName,
    List<UUID> teamMemberAthleteIds
) {
}
