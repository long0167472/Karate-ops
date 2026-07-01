package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.LeaveRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestCreateRequest(
    @NotNull LeaveRequestType requestType,
    UUID sessionId,
    LocalDate fromDate,
    LocalDate toDate,
    @NotBlank String reason
) {
}
