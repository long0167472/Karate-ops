package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.BeltExamStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record BeltExamCreateRequest(
    @NotBlank String name,
    BeltExamStatus status,
    LocalDate examDate,
    String location,
    String examinerName,
    String notes
) {
}
