package com.karate.tournament.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BeltExamScoreRequest(
    @NotNull BigDecimal score,
    String note
) {
}
