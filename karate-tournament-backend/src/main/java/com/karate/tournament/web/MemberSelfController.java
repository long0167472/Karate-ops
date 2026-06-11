package com.karate.tournament.web;

import com.karate.tournament.dto.request.LeaveRequestCreateRequest;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.dto.response.MemberAttendanceSummaryResponse;
import com.karate.tournament.dto.response.MemberClubProfileResponse;
import com.karate.tournament.dto.response.MemberFeeSummaryResponse;
import com.karate.tournament.service.AttendanceLeaveRequestService;
import com.karate.tournament.service.MemberSelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MemberSelfController {
  private final MemberSelfService memberSelfService;
  private final AttendanceLeaveRequestService leaveRequests;

  @GetMapping("/club-profile")
  public MemberClubProfileResponse clubProfile() {
    return memberSelfService.clubProfile();
  }

  @GetMapping("/fees")
  public MemberFeeSummaryResponse fees() {
    return memberSelfService.fees();
  }

  @GetMapping("/attendance")
  public MemberAttendanceSummaryResponse attendance() {
    return memberSelfService.attendance();
  }

  @PostMapping("/attendance/leave-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public LeaveRequestResponse createLeaveRequest(@Valid @RequestBody LeaveRequestCreateRequest request) {
    return leaveRequests.create(request);
  }
}
