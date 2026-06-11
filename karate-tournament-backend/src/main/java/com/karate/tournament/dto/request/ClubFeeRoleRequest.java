package com.karate.tournament.dto.request;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubFeeRoleRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    Integer priority,
    Boolean active
) {
}
