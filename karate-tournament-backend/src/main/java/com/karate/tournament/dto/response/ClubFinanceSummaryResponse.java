package com.karate.tournament.dto.response;

import java.math.BigDecimal;

public record ClubFinanceSummaryResponse(
    long activeMembers,
    BigDecimal monthlyTuitionExpected,
    BigDecimal oneTimeIncomeDue,
    BigDecimal totalReceivable,
    BigDecimal totalPaid,
    BigDecimal totalOutstanding,
    BigDecimal expensesTotal,
    BigDecimal expensesDisbursed,
    BigDecimal expensesPending,
    BigDecimal netCash
) {
}
