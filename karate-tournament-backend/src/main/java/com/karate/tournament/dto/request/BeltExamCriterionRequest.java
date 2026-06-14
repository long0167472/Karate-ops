package com.karate.tournament.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record BeltExamCriterionRequest(
    @NotBlank String name,
    String description,
    BigDecimal maxScore,
    BigDecimal weight,
    Integer displayOrder
) {
}
