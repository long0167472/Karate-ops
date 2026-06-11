package com.karate.tournament.dto.request;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ApplyFeeItemRequest(
    List<UUID> memberIds,
    List<UUID> feeRoleIds,
    Boolean applyToAllActive,
    LocalDate dueDate,
    String note
) {
}
