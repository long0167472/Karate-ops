package com.karate.tournament.dto.response;

import java.util.UUID;

public record AthleteRankingResponse(
    int rank,
    UUID athleteId,
    String athleteName,
    UUID organizationId,
    String organizationName,
    int points
) {}
