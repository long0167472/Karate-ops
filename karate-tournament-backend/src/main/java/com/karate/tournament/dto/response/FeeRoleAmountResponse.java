package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record FeeRoleAmountResponse(
    UUID id,
    UUID feeRoleId,
    String feeRoleName,
    BigDecimal amount,
    boolean exempt
) {
}
