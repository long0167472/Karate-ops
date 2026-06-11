package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MemberAttendanceSessionResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    String name,
    AttendanceSessionType type,
    AttendanceSessionStatus status,
    Instant scheduledAt,
    LocalDate scheduledDate,
    AttendanceRecordResponse record,
    LeaveRequestResponse leaveRequest
) {
}
