package com.karate.tournament.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberTuitionOverrideResponse(
    UUID memberId,
    String memberName,
    UUID feeItemId,
    String feeItemName,
    BigDecimal amount
) {
}
