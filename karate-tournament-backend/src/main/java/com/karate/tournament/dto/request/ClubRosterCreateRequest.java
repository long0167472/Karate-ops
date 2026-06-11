package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.ClubRosterStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubRosterCreateRequest(
    @NotNull UUID athleteId,
    ClubRosterStatus status,
    LocalDate joinedAt
) {
}
