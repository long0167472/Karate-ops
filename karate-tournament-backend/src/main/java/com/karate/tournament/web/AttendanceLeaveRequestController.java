package com.karate.tournament.web;

import com.karate.tournament.dto.request.LeaveRequestDecisionRequest;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.service.AttendanceLeaveRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttendanceLeaveRequestController {
  private final AttendanceLeaveRequestService leaveRequests;

  @GetMapping("/organizations/{organizationId}/attendance-leave-requests")
  public List<LeaveRequestResponse> list(@PathVariable UUID organizationId) {
    return leaveRequests.listByOrganization(organizationId);
  }

  @PatchMapping("/attendance-leave-requests/{requestId}/decision")
  public LeaveRequestResponse decide(
      @PathVariable UUID requestId,
      @Valid @RequestBody LeaveRequestDecisionRequest request
  ) {
    return leaveRequests.decide(requestId, request);
  }
}
