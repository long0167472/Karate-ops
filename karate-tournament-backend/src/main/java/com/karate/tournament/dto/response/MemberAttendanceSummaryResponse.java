package com.karate.tournament.dto.response;

import java.util.List;

public record MemberAttendanceSummaryResponse(
    long sessions,
    long present,
    long late,
    long absent,
    long excused,
    long pendingLeaveRequests,
    List<MemberAttendanceSessionResponse> sessionRows
) {
}
