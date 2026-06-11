package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.AthleteStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AthleteCreateRequest(
    @NotNull UUID personId,
    UUID primaryOrganizationId,
    String externalCode,
    String belt,
    BigDecimal weightKg,
    BigDecimal heightCm,
    AthleteStatus status
) {
}
