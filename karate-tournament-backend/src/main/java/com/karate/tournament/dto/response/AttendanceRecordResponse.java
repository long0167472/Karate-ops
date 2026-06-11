package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.AttendanceRecordStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AttendanceRecordResponse(
    UUID id,
    UUID sessionId,
    UUID organizationMemberId,
    UUID athleteId,
    UUID personId,
    String displayName,
    AttendanceRecordStatus status,
    Instant checkInAt,
    String note
) {
}
