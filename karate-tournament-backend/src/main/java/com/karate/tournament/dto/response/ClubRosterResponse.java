package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.ClubRosterStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubRosterResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    UUID athleteId,
    String athleteName,
    UUID personId,
    ClubRosterStatus status,
    LocalDate joinedAt
) {
}
