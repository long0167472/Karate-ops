package com.karate.tournament.dto.request;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubTrainingScheduleRequest(
    @NotBlank String name,
    List<Integer> daysOfWeek,
    String startTime,
    Integer durationMinutes,
    String timezone,
    Boolean active
) {
}
