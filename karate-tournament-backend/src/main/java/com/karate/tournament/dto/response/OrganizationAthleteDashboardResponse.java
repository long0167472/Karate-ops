package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record OrganizationAthleteDashboardResponse(
    UUID organizationId,
    UUID athleteId,
    String athleteName,
    long sessions,
    long present,
    long late,
    long absent,
    long excused,
    double attendanceRate,
    long tournamentEntries,
    List<String> tournaments
) {
}
