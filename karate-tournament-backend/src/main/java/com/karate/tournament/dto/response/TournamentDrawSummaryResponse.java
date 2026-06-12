package com.karate.tournament.dto.response;

import java.util.List;
import java.util.UUID;

public record TournamentDrawSummaryResponse(
    List<CategoryDrawResponse> categories
) {
  public record CategoryDrawResponse(
      UUID categoryId,
      String categoryName,
      int athleteCount,
      int bracketSize,
      boolean hasActiveDraw
  ) {
  }
}
