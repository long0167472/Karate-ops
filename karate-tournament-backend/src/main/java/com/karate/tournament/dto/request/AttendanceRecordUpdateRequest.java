package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.AttendanceRecordStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AttendanceRecordUpdateRequest(
    AttendanceRecordStatus status,
    Instant checkInAt,
    String note
) {
}
