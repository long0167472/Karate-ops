package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.EntryStatus;
import com.karate.tournament.entity.enums.WeighInStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record EntryResponse(
    UUID id,
    UUID categoryId,
    UUID tournamentParticipantId,
    String participantName,
    UUID athleteId,
    String athleteName,
    UUID teamId,
    Integer seedNo,
    EntryStatus status,
    BigDecimal registrationWeightKg,
    WeighInStatus weighInStatus,
    String teamName,
    List<UUID> teamMemberAthleteIds,
    String validationNotes
) {
}
