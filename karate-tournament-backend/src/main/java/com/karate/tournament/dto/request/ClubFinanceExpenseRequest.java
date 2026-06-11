package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.ExpenseDisbursementStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClubFinanceExpenseRequest(
    @NotBlank String name,
    BigDecimal amount,
    LocalDate expenseDate,
    ExpenseDisbursementStatus status,
    String note
) {
}
