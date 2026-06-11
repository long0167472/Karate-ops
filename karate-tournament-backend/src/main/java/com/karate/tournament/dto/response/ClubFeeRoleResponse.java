package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubFeeRoleResponse(
    UUID id,
    UUID organizationId,
    String code,
    String name,
    String description,
    int priority,
    boolean active
) {
}
