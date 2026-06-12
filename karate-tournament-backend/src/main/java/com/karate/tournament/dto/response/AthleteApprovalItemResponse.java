package com.karate.tournament.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AthleteApprovalItemResponse(
    UUID entryId,
    UUID athleteId,
    String athleteName,
    UUID organizationId,
    String organizationName,
    UUID categoryId,
    String categoryName,
    BigDecimal registrationWeightKg,
    String btcApprovalStatus
) {}
