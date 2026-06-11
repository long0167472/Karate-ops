package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record LowAttendanceAthleteRow(
    UUID athleteId,
    String athleteName,
    long sessions,
    long presentOrLate,
    double attendanceRate
) {
}
