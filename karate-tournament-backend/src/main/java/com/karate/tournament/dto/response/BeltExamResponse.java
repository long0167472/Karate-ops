package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.BeltExamStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BeltExamResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    String name,
    BeltExamStatus status,
    LocalDate examDate,
    String location,
    String examinerName,
    BigDecimal passThreshold,
    String notes,
    List<BeltExamCriterionResponse> criteria,
    List<BeltExamCandidateResponse> candidates
) {
}
