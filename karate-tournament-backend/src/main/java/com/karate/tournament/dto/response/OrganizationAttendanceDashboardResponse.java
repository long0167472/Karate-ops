package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record OrganizationAttendanceDashboardResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long sessions,
    long records,
    long present,
    long absent,
    long late,
    long excused,
    double attendanceRate,
    List<LowAttendanceAthleteRow> lowAttendance
) {
}
