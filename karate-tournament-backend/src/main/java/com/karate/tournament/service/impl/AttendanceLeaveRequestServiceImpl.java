package com.karate.tournament.service.impl;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.LeaveRequestCreateRequest;
import com.karate.tournament.dto.request.LeaveRequestDecisionRequest;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.AttendanceLeaveRequest;
import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.AttendanceLeaveRequestRepository;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.AttendanceSessionRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.service.AttendanceLeaveRequestService;
import com.karate.tournament.web.ApiMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceLeaveRequestServiceImpl implements AttendanceLeaveRequestService {
  private final AttendanceLeaveRequestRepository leaveRequests;
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final OrganizationMemberRepository members;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional
  public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
    CurrentActor actor = permissions.currentActor();
    AttendanceSession session = sessions.findByIdAndDeletedAtIsNull(request.sessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found: " + request.sessionId()));
    if (session.status == AttendanceSessionStatus.CANCELLED) {
      throw new BusinessConflictException("Cannot request leave for a cancelled session");
    }
    OrganizationMember member = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId()).stream()
        .filter(row -> row.organization.id.equals(session.organization.id))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Current user is not a member of this session organization"));
    permissions.requireSelfOrClubManage(session.organization.id, actor.userId());
    AttendanceLeaveRequest leaveRequest = leaveRequests.findBySession_IdAndMember_IdAndDeletedAtIsNull(session.id, member.id)
        .orElseGet(AttendanceLeaveRequest::create);
    if (leaveRequest.id != null && leaveRequest.status != LeaveRequestStatus.PENDING) {
      throw new BusinessConflictException("Leave request was already decided");
    }
    leaveRequest.session = session;
    leaveRequest.member = member;
    leaveRequest.requesterUser = requireUser(actor.userId());
    leaveRequest.status = LeaveRequestStatus.PENDING;
    leaveRequest.reason = request.reason().trim();
    leaveRequest.decisionNote = null;
    leaveRequest.decidedAt = null;
    leaveRequest.decidedByUser = null;
    return mapper.leaveRequest(leaveRequests.save(leaveRequest));
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> listByOrganization(UUID organizationId) {
    permissions.requireAttendanceManage(organizationId);
    return leaveRequests.findBySession_Organization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::leaveRequest)
        .toList();
  }

  @Transactional
  public LeaveRequestResponse decide(UUID requestId, LeaveRequestDecisionRequest request) {
    if (request.status() == LeaveRequestStatus.PENDING) {
      throw new BadRequestException("Decision status must be APPROVED or REJECTED");
    }
    AttendanceLeaveRequest leaveRequest = leaveRequests.findByIdAndDeletedAtIsNull(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + requestId));
    permissions.requireAttendanceManage(leaveRequest.session.organization.id);
    leaveRequest.status = request.status();
    leaveRequest.decisionNote = request.decisionNote();
    leaveRequest.decidedAt = Instant.now();
    leaveRequest.decidedByUser = requireUser(permissions.currentActor().userId());
    if (request.status() == LeaveRequestStatus.APPROVED) {
      approveAttendanceRecord(leaveRequest);
    }
    return mapper.leaveRequest(leaveRequest);
  }

  private void approveAttendanceRecord(AttendanceLeaveRequest leaveRequest) {
    AttendanceRecord record = records.findBySession_IdAndOrganizationMember_IdAndDeletedAtIsNull(
            leaveRequest.session.id,
            leaveRequest.member.id
        )
        .orElseGet(AttendanceRecord::create);
    record.session = leaveRequest.session;
    record.organizationMember = leaveRequest.member;
    record.status = AttendanceRecordStatus.EXCUSED;
    record.note = "Leave approved: " + leaveRequest.reason;
    records.save(record);
  }

  private AppUser requireUser(UUID userId) {
    return users.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
  }
}
