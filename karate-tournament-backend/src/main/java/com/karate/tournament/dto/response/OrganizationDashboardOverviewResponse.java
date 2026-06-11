package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record OrganizationDashboardOverviewResponse(
    UUID organizationId,
    String organizationName,
    long activeMembers,
    long activeAthletes,
    long attendanceSessions,
    long tournamentEntries,
    double attendanceRate
) {
}
