package com.karate.tournament.service;

import com.karate.tournament.dto.request.LeaveRequestCreateRequest;
import com.karate.tournament.dto.request.LeaveRequestDecisionRequest;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import java.util.List;
import java.util.UUID;

public interface AttendanceLeaveRequestService {
  LeaveRequestResponse create(LeaveRequestCreateRequest request);
  List<LeaveRequestResponse> listByOrganization(UUID organizationId);
  List<LeaveRequestResponse> listForCurrentUser();
  LeaveRequestResponse decide(UUID requestId, LeaveRequestDecisionRequest request);
  LeaveRequestResponse decideForOrganization(UUID organizationId, UUID requestId, LeaveRequestDecisionRequest request);
  void expireOverdueRequests();
}
