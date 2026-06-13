package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.BeltExamStatus;
import java.time.LocalDate;

public record BeltExamUpdateRequest(
    String name,
    BeltExamStatus status,
    LocalDate examDate,
    String location,
    String examinerName,
    String notes
) {
}
