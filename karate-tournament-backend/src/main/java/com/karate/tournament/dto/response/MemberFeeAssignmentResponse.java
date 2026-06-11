package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.FeeAssignmentSource;
import com.karate.tournament.entity.enums.PaymentStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MemberFeeAssignmentResponse(
    UUID id,
    UUID organizationId,
    UUID memberId,
    String memberName,
    UUID feeItemId,
    String feeItemName,
    UUID assignedRoleId,
    String assignedRoleName,
    BigDecimal amountDue,
    BigDecimal paidAmount,
    PaymentStatus status,
    LocalDate dueDate,
    FeeAssignmentSource source,
    String note
) {
}
