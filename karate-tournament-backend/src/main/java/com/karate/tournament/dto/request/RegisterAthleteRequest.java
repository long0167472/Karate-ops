package com.karate.tournament.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterAthleteRequest(
    UUID categoryId,
    UUID athleteId,
    BigDecimal registrationWeightKg
) {}
