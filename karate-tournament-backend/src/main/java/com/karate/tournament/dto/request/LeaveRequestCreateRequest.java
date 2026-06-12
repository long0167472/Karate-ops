package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.LeaveRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestCreateRequest(
    LeaveRequestType requestType,
    UUID sessionId,
    UUID organizationId,
    LocalDate fromDate,
    LocalDate toDate,
    @NotBlank @Size(max = 500) String reason
) {
}
