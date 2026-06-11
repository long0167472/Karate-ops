package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.RulesetVersion;
import com.karate.tournament.entity.enums.RulesetPreset;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.entity.enums.TournamentVisibility;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record TournamentUpdateRequest(
    String name,
    String code,
    String description,
    String location,
    LocalDate startsOn,
    LocalDate endsOn,
    UUID ownerOrganizationId,
    TournamentVisibility visibility,
    TournamentStatus status,
    RulesetVersion rulesetVersion,
    String organizerName,
    Integer tatamiCount,
    List<String> competitionLevels,
    RulesetPreset rulesetPreset,
    String ruleSnapshotJson
) {
}
