package com.karate.tournament.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MemberFeeSummaryResponse(
    BigDecimal totalDue,
    BigDecimal totalPaid,
    BigDecimal totalRemaining,
    List<MemberFeeAssignmentResponse> assignments
) {
}
