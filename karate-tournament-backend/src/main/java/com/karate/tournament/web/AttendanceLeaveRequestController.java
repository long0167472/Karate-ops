package com.karate.tournament.web;

import com.karate.tournament.dto.request.DecisionNoteRequest;
import com.karate.tournament.dto.request.LeaveRequestDecisionRequest;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
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

  @GetMapping("/organizations/{organizationId}/leave-requests")
  public List<LeaveRequestResponse> listForOrganization(@PathVariable UUID organizationId) {
    return leaveRequests.listByOrganization(organizationId);
  }

  @PatchMapping("/organizations/{organizationId}/leave-requests/{requestId}/approve")
  public LeaveRequestResponse approve(
      @PathVariable UUID organizationId,
      @PathVariable UUID requestId,
      @Valid @RequestBody(required = false) DecisionNoteRequest request
  ) {
    return leaveRequests.decideForOrganization(organizationId, requestId,
        new LeaveRequestDecisionRequest(LeaveRequestStatus.APPROVED, request == null ? null : request.decisionNote()));
  }

  @PatchMapping("/organizations/{organizationId}/leave-requests/{requestId}/reject")
  public LeaveRequestResponse reject(
      @PathVariable UUID organizationId,
      @PathVariable UUID requestId,
      @Valid @RequestBody(required = false) DecisionNoteRequest request
  ) {
    return leaveRequests.decideForOrganization(organizationId, requestId,
        new LeaveRequestDecisionRequest(LeaveRequestStatus.REJECTED, request == null ? null : request.decisionNote()));
  }

  // Legacy routes kept for compatibility with earlier clients.
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
