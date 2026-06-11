package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.OrganizationStatus;
import com.karate.tournament.entity.enums.OrganizationType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record OrganizationResponse(
    UUID id,
    String name,
    String shortName,
    String code,
    OrganizationType type,
    OrganizationStatus status,
    String country,
    String province,
    String address,
    String contactEmail,
    String contactPhone
) {
}
