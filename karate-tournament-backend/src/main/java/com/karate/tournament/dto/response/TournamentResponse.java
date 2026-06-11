package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.RulesetVersion;
import com.karate.tournament.entity.enums.RulesetPreset;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.entity.enums.TournamentVisibility;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record TournamentResponse(
    UUID id,
    String name,
    String code,
    String description,
    String location,
    LocalDate startsOn,
    LocalDate endsOn,
    TournamentVisibility visibility,
    TournamentStatus status,
    RulesetVersion rulesetVersion,
    String organizerName,
    Integer tatamiCount,
    List<String> competitionLevels,
    RulesetPreset rulesetPreset,
    String ruleSnapshotJson,
    UUID ownerOrganizationId,
    String ownerOrganizationName,
    UUID createdByUserId
) {
}
