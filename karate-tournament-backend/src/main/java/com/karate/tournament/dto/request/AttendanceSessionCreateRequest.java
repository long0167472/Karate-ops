package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AttendanceSessionCreateRequest(
    @NotBlank String name,
    AttendanceSessionType type,
    AttendanceSessionStatus status,
    Instant scheduledAt,
    UUID tournamentParticipantId,
    String notes
) {
}
