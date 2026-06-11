package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.CompetitionLevel;
import com.karate.tournament.entity.enums.EntryType;
import com.karate.tournament.entity.enums.PersonGender;
import com.karate.tournament.entity.enums.RulesetVersion;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record CategoryResponse(
    UUID id,
    UUID tournamentId,
    String name,
    CategoryDiscipline discipline,
    PersonGender gender,
    Integer ageMin,
    Integer ageMax,
    BigDecimal weightMinKg,
    BigDecimal weightMaxKg,
    CompetitionLevel competitionLevel,
    String weightLabel,
    Boolean openWeight,
    EntryType entryType,
    String status,
    RulesetVersion rulesetVersion,
    Boolean repechageEnabled,
    Integer matchDurationSeconds,
    Integer kataJudgeCount,
    Boolean kataRepeatAllowed,
    Integer entryLimitPerOrganization
) {
}
