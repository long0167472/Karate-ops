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
import com.karate.tournament.entity.enums.LeaveRequestType;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceLeaveRequestServiceImpl implements AttendanceLeaveRequestService {
  private static final Duration DECISION_WINDOW = Duration.ofHours(24);

  private final AttendanceLeaveRequestRepository leaveRequests;
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final OrganizationMemberRepository members;
  private final AppUserRepository users;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional
  public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
    LeaveRequestType requestType = request.requestType() == null ? LeaveRequestType.LEAVE_SESSION : request.requestType();
    if (requestType == LeaveRequestType.LEAVE_LONG_TERM) {
      return createLongTermLeave(request);
    }
    return createSessionLeave(request, requestType);
  }

  private LeaveRequestResponse createSessionLeave(LeaveRequestCreateRequest request, LeaveRequestType requestType) {
    if (request.sessionId() == null) {
      throw new BadRequestException("sessionId is required for session-scoped requests");
    }
    CurrentActor actor = permissions.currentActor();
    AttendanceSession session = sessions.findByIdAndDeletedAtIsNull(request.sessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found: " + request.sessionId()));
    if (session.status == AttendanceSessionStatus.CANCELLED) {
      throw new BusinessConflictException("Cannot request leave for a cancelled session");
    }
    OrganizationMember member = requireMembership(actor.userId(), session.organization.id);
    permissions.requireSelfOrClubManage(session.organization.id, actor.userId());
    // Pessimistic lock: re-submission cannot race a concurrent manager decision on the same row.
    AttendanceLeaveRequest leaveRequest = leaveRequests.findWithLockBySessionAndMember(session.id, member.id)
        .orElseGet(AttendanceLeaveRequest::create);
    if (leaveRequest.id != null && leaveRequest.status != LeaveRequestStatus.PENDING) {
      throw new BusinessConflictException("Leave request was already decided");
    }
    leaveRequest.session = session;
    leaveRequest.organization = session.organization;
    leaveRequest.member = member;
    leaveRequest.requesterUser = requireUser(actor.userId());
    leaveRequest.requestType = requestType;
    leaveRequest.status = LeaveRequestStatus.PENDING;
    leaveRequest.reason = request.reason().trim();
    leaveRequest.fromDate = null;
    leaveRequest.toDate = null;
    leaveRequest.expiresAt = Instant.now().plus(DECISION_WINDOW);
    leaveRequest.decisionNote = null;
    leaveRequest.decidedAt = null;
    leaveRequest.decidedByUser = null;
    return mapper.leaveRequest(saveGuardingDuplicates(leaveRequest));
  }

  private LeaveRequestResponse createLongTermLeave(LeaveRequestCreateRequest request) {
    if (request.fromDate() == null || request.toDate() == null) {
      throw new BadRequestException("fromDate and toDate are required for long-term leave");
    }
    if (request.toDate().isBefore(request.fromDate())) {
      throw new BadRequestException("toDate must not be before fromDate");
    }
    CurrentActor actor = permissions.currentActor();
    OrganizationMember member = resolveMembership(actor.userId(), request.organizationId());
    permissions.requireSelfOrClubManage(member.organization.id, actor.userId());
    boolean overlapping = leaveRequests
        .findByMember_IdAndRequestTypeAndStatusAndDeletedAtIsNull(member.id, LeaveRequestType.LEAVE_LONG_TERM, LeaveRequestStatus.PENDING)
        .stream()
        .anyMatch(existing -> existing.fromDate != null && existing.toDate != null
            && !existing.toDate.isBefore(request.fromDate())
            && !existing.fromDate.isAfter(request.toDate()));
    if (overlapping) {
      throw new BusinessConflictException("A pending long-term leave request already covers this period");
    }
    AttendanceLeaveRequest leaveRequest = AttendanceLeaveRequest.create();
    leaveRequest.session = null;
    leaveRequest.organization = member.organization;
    leaveRequest.member = member;
    leaveRequest.requesterUser = requireUser(actor.userId());
    leaveRequest.requestType = LeaveRequestType.LEAVE_LONG_TERM;
    leaveRequest.status = LeaveRequestStatus.PENDING;
    leaveRequest.reason = request.reason().trim();
    leaveRequest.fromDate = request.fromDate();
    leaveRequest.toDate = request.toDate();
    leaveRequest.expiresAt = Instant.now().plus(DECISION_WINDOW);
    return mapper.leaveRequest(saveGuardingDuplicates(leaveRequest));
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> listByOrganization(UUID organizationId) {
    permissions.requireAttendanceManage(organizationId);
    return leaveRequests.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::leaveRequest)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> listForCurrentUser() {
    CurrentActor actor = permissions.currentActor();
    permissions.requireMemberSelfView(actor.userId());
    List<UUID> memberIds = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId())
        .stream()
        .map(member -> member.id)
        .toList();
    if (memberIds.isEmpty()) {
      return List.of();
    }
    return leaveRequests.findByMember_IdInAndDeletedAtIsNullOrderByCreatedAtDesc(memberIds)
        .stream()
        .map(mapper::leaveRequest)
        .toList();
  }

  @Transactional
  public LeaveRequestResponse decide(UUID requestId, LeaveRequestDecisionRequest request) {
    return decideInternal(null, requestId, request);
  }

  @Transactional
  public LeaveRequestResponse decideForOrganization(UUID organizationId, UUID requestId, LeaveRequestDecisionRequest request) {
    return decideInternal(organizationId, requestId, request);
  }

  private LeaveRequestResponse decideInternal(UUID organizationId, UUID requestId, LeaveRequestDecisionRequest request) {
    if (request.status() != LeaveRequestStatus.APPROVED && request.status() != LeaveRequestStatus.REJECTED) {
      throw new BadRequestException("Decision status must be APPROVED or REJECTED");
    }
    // Pessimistic lock: concurrent decisions serialize here; the loser sees a non-PENDING
    // status below and fails instead of double-writing attendance records.
    AttendanceLeaveRequest leaveRequest = leaveRequests.findWithLockById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + requestId));
    if (organizationId != null && !leaveRequest.organization.id.equals(organizationId)) {
      throw new ResourceNotFoundException("Leave request does not belong to organization");
    }
    permissions.requireAttendanceManage(leaveRequest.organization.id);
    if (leaveRequest.status != LeaveRequestStatus.PENDING) {
      throw new BusinessConflictException("Leave request was already decided");
    }
    leaveRequest.status = request.status();
    leaveRequest.decisionNote = request.decisionNote();
    leaveRequest.decidedAt = Instant.now();
    leaveRequest.decidedByUser = requireUser(permissions.currentActor().userId());
    if (request.status() == LeaveRequestStatus.APPROVED) {
      applyApprovedLeave(leaveRequest);
    }
    return mapper.leaveRequest(leaveRequest);
  }

  /**
   * Marks PENDING requests past their decision window as EXPIRED_AUTO_ABSENT and records the
   * member absent for the session, as promised to members when they submit the request.
   */
  @Transactional
  public void expireOverdueRequests() {
    List<AttendanceLeaveRequest> overdue =
        leaveRequests.findByStatusAndExpiresAtBeforeAndDeletedAtIsNull(LeaveRequestStatus.PENDING, Instant.now());
    for (AttendanceLeaveRequest stale : overdue) {
      // Lock each row before mutating: a manager may be approving it at this very moment.
      AttendanceLeaveRequest locked = leaveRequests.findWithLockById(stale.id).orElse(null);
      if (locked == null || locked.status != LeaveRequestStatus.PENDING) {
        continue;
      }
      locked.status = LeaveRequestStatus.EXPIRED_AUTO_ABSENT;
      locked.decidedAt = Instant.now();
      if (locked.session != null) {
        upsertRecord(locked, AttendanceRecordStatus.ABSENT, "Leave request expired without decision");
      }
    }
  }

  private void applyApprovedLeave(AttendanceLeaveRequest leaveRequest) {
    if (leaveRequest.requestType == LeaveRequestType.LEAVE_SESSION && leaveRequest.session != null) {
      upsertRecord(leaveRequest, AttendanceRecordStatus.EXCUSED, "Leave approved: " + leaveRequest.reason);
      return;
    }
    if (leaveRequest.requestType == LeaveRequestType.LATE && leaveRequest.session != null) {
      upsertRecord(leaveRequest, AttendanceRecordStatus.LATE, "Late arrival approved: " + leaveRequest.reason);
      return;
    }
    if (leaveRequest.requestType == LeaveRequestType.LEAVE_LONG_TERM
        && leaveRequest.fromDate != null && leaveRequest.toDate != null) {
      sessions.findByOrganization_IdAndScheduledDateBetweenAndDeletedAtIsNullOrderByScheduledAtAscCreatedAtAsc(
              leaveRequest.organization.id, leaveRequest.fromDate, leaveRequest.toDate)
          .stream()
          .filter(session -> session.status != AttendanceSessionStatus.CANCELLED)
          .forEach(session -> upsertRecord(leaveRequest, session, AttendanceRecordStatus.EXCUSED,
              "Long-term leave approved: " + leaveRequest.reason));
    }
  }

  private void upsertRecord(AttendanceLeaveRequest leaveRequest, AttendanceRecordStatus status, String note) {
    upsertRecord(leaveRequest, leaveRequest.session, status, note);
  }

  private void upsertRecord(AttendanceLeaveRequest leaveRequest, AttendanceSession session, AttendanceRecordStatus status, String note) {
    AttendanceRecord record = records.findBySession_IdAndOrganizationMember_IdAndDeletedAtIsNull(session.id, leaveRequest.member.id)
        .orElseGet(AttendanceRecord::create);
    record.session = session;
    record.organizationMember = leaveRequest.member;
    record.status = status;
    record.note = note;
    records.save(record);
  }

  private AttendanceLeaveRequest saveGuardingDuplicates(AttendanceLeaveRequest leaveRequest) {
    try {
      // Flush inside the transaction so the partial unique index rejects a concurrent
      // duplicate here, where it can be translated to a business conflict.
      return leaveRequests.saveAndFlush(leaveRequest);
    } catch (DataIntegrityViolationException ex) {
      throw new BusinessConflictException("A leave request for this session already exists");
    }
  }

  private OrganizationMember requireMembership(UUID userId, UUID organizationId) {
    return members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
        .filter(row -> row.organization.id.equals(organizationId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Current user is not a member of this organization"));
  }

  private OrganizationMember resolveMembership(UUID userId, UUID organizationId) {
    List<OrganizationMember> memberships = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    if (organizationId != null) {
      return memberships.stream()
          .filter(row -> row.organization.id.equals(organizationId))
          .findFirst()
          .orElseThrow(() -> new ResourceNotFoundException("Current user is not a member of this organization"));
    }
    return memberships.stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Current user has no club membership"));
  }

  private AppUser requireUser(UUID userId) {
    return users.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
  }
}
