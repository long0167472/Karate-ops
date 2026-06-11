package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.AthleteStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AthleteResponse(
    UUID id,
    UUID personId,
    String displayName,
    UUID primaryOrganizationId,
    String primaryOrganizationName,
    String externalCode,
    String belt,
    BigDecimal weightKg,
    BigDecimal heightCm,
    AthleteStatus status
) {
}
