package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.AccountRequestStatus;
import jakarta.validation.constraints.NotNull;

public record AccountRequestDecisionRequest(
    @NotNull AccountRequestStatus status,
    String decisionNote
) {
}
