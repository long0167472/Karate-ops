package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.OrganizationType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record OrganizationCreateRequest(
    @NotBlank String name,
    String shortName,
    String code,
    @NotNull OrganizationType type,
    String country,
    String province,
    String address,
    String contactEmail,
    String contactPhone
) {
}
