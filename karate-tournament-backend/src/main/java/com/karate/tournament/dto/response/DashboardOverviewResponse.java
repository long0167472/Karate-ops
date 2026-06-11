package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record DashboardOverviewResponse(
    UUID tournamentId,
    long organizations,
    long athletes,
    long categories,
    long matches,
    long completedMatches,
    Map<String, Long> matchesByStatus
) {
}
