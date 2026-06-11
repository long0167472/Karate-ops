package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AttendanceSessionResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    UUID tournamentParticipantId,
    String name,
    AttendanceSessionType type,
    AttendanceSessionStatus status,
    Instant scheduledAt,
    String source,
    LocalDate scheduledDate,
    UUID trainingScheduleId,
    String notes,
    List<AttendanceRecordResponse> records
) {
}
