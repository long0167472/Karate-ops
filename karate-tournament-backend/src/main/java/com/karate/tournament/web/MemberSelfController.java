package com.karate.tournament.web;

import com.karate.tournament.dto.request.LeaveRequestCreateRequest;
import com.karate.tournament.dto.request.TournamentJoinRequestCreateRequest;
import com.karate.tournament.dto.response.ClubAnnouncementResponse;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.dto.response.MemberAttendanceSummaryResponse;
import com.karate.tournament.dto.response.MemberClubProfileResponse;
import com.karate.tournament.dto.response.MemberFeeSummaryResponse;
import com.karate.tournament.dto.response.TournamentJoinRequestResponse;
import com.karate.tournament.service.AttendanceLeaveRequestService;
import com.karate.tournament.service.ClubAnnouncementService;
import com.karate.tournament.service.MemberSelfService;
import com.karate.tournament.service.TournamentJoinRequestService;
import jakarta.validation.Valid;
import java.util.List;
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
  private final ClubAnnouncementService announcements;
  private final TournamentJoinRequestService joinRequests;

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

  @GetMapping("/announcements")
  public List<ClubAnnouncementResponse> announcements() {
    return announcements.listForCurrentUser();
  }

  @GetMapping("/leave-requests")
  public List<LeaveRequestResponse> myLeaveRequests() {
    return leaveRequests.listForCurrentUser();
  }

  @PostMapping("/attendance/leave-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public LeaveRequestResponse createLeaveRequest(@Valid @RequestBody LeaveRequestCreateRequest request) {
    return leaveRequests.create(request);
  }

  @GetMapping("/tournament-join-requests")
  public List<TournamentJoinRequestResponse> myJoinRequests() {
    return joinRequests.listForCurrentUser();
  }

  @PostMapping("/tournament-join-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public TournamentJoinRequestResponse createJoinRequest(@Valid @RequestBody TournamentJoinRequestCreateRequest request) {
    return joinRequests.createForCurrentUser(request);
  }
}
