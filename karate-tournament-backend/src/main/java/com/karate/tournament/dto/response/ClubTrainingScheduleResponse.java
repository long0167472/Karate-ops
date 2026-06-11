package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubTrainingScheduleResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    String name,
    List<Integer> daysOfWeek,
    String startTime,
    int durationMinutes,
    String timezone,
    boolean active
) {
}
