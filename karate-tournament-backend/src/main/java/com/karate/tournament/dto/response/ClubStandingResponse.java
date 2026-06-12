package com.karate.tournament.dto.response;

import java.util.UUID;

public record ClubStandingResponse(
    UUID organizationId,
    String organizationName,
    int totalPoints,
    int goldMedals,
    int silverMedals,
    int bronzeMedals,
    int medalScore
) {
  public static ClubStandingResponse of(
      UUID organizationId,
      String organizationName,
      int totalPoints,
      int goldMedals,
      int silverMedals,
      int bronzeMedals
  ) {
    int medalScore = goldMedals * 3 + silverMedals * 2 + bronzeMedals;
    return new ClubStandingResponse(
        organizationId, organizationName, totalPoints,
        goldMedals, silverMedals, bronzeMedals, medalScore
    );
  }
}
