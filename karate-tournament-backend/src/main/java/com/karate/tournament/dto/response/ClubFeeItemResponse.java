package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.BillingCycle;
import com.karate.tournament.entity.enums.FeeItemKind;
import com.karate.tournament.entity.enums.FeeItemStatus;
import com.karate.tournament.entity.enums.FeeItemType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubFeeItemResponse(
    UUID id,
    UUID organizationId,
    String name,
    FeeItemType feeType,
    FeeItemKind feeKind,
    BillingCycle billingCycle,
    FeeItemStatus status,
    BigDecimal defaultAmount,
    Integer dueDay,
    String description,
    List<FeeRoleAmountResponse> roleAmounts
) {
}
