package com.karate.tournament.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TournamentRegistrationResponse(
    UUID participantId,
    UUID organizationId,
    String displayName,
    String status,
    Instant registeredAt,
    List<RegistrationEntryItem> entries
) {
  public record RegistrationEntryItem(
      UUID entryId,
      UUID categoryId,
      String categoryName,
      UUID athleteId,
      String athleteName,
      BigDecimal registrationWeightKg,
      String btcApprovalStatus
  ) {}
}
