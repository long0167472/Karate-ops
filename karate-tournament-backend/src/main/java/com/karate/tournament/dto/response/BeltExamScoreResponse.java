package com.karate.tournament.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BeltExamScoreResponse(
    UUID id,
    UUID candidateId,
    UUID criterionId,
    BigDecimal score,
    String note
) {
}
