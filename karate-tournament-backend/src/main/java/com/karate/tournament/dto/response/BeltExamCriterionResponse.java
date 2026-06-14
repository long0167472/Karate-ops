package com.karate.tournament.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BeltExamCriterionResponse(
    UUID id,
    UUID examId,
    String name,
    String description,
    BigDecimal maxScore,
    BigDecimal weight,
    int displayOrder
) {
}
