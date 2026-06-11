package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.ExpenseDisbursementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClubFinanceExpenseResponse(
    UUID id,
    UUID organizationId,
    String name,
    BigDecimal amount,
    LocalDate expenseDate,
    ExpenseDisbursementStatus status,
    String note
) {
}
