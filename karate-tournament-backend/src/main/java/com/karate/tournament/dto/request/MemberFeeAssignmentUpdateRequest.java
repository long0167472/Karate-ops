package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.PaymentStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MemberFeeAssignmentUpdateRequest(
    BigDecimal amountDue,
    BigDecimal paidAmount,
    PaymentStatus status,
    LocalDate dueDate,
    String note
) {
}
