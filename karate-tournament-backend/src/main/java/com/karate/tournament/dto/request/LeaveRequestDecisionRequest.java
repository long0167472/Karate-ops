package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.LeaveRequestStatus;
import jakarta.validation.constraints.NotNull;

public record LeaveRequestDecisionRequest(
    @NotNull LeaveRequestStatus status,
    String decisionNote
) {
}
