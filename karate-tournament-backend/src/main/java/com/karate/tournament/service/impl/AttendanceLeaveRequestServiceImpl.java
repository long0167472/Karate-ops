package com.karate.tournament.service.impl;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.LeaveRequestCreateRequest;
import com.karate.tournament.dto.request.LeaveRequestDecisionRequest;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.AttendanceLeaveRequest;
import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
import com.karate.tournament.entity.enums.LeaveRequestType;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.AttendanceLeaveRequestRepository;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.AttendanceSessionRepository;
import com.karate.tournament.repository.ClubRosterRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.service.AttendanceLeaveRequestService;
import com.karate.tournament.web.ApiMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceLeaveRequestServiceImpl implements AttendanceLeaveRequestService {
  private static final ZoneId CLUB_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final AttendanceLeaveRequestRepository leaveRequests;
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final OrganizationMemberRepository members;
  private final AppUserRepository users;
  private final AthleteRepository athletes;
  private final ClubRosterRepository roster;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional
  public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
    CurrentActor actor = permissions.currentActor();
    AttendanceLeaveRequest leaveRequest = request.requestType() == LeaveRequestType.LEAVE_LONG_TERM
        ? createLongTermRequest(actor, request)
        : createSessionBasedRequest(actor, request);
    return mapper.leaveRequest(leaveRequest);
  }

  @Transactional(readOnly = true)
  public List<LeaveRequestResponse> listByOrganization(UUID organizationId) {
    permissions.requireAttendanceManage(organizationId);
    return leaveRequests.findByMember_Organization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::leaveRequest)
        .toList();
  }

  @Transactional
  public LeaveRequestResponse decide(UUID requestId, LeaveRequestDecisionRequest request) {
    if (request.status() == LeaveRequestStatus.PENDING || request.status() == LeaveRequestStatus.EXPIRED_AUTO_ABSENT) {
      throw new BadRequestException("Decision status must be APPROVED or REJECTED");
    }
    AttendanceLeaveRequest leaveRequest = requireLeaveRequest(requestId);
    if (leaveRequest.status != LeaveRequestStatus.PENDING) {
      throw new BusinessConflictException("Leave request was already decided");
    }
    permissions.requireAttendanceManage(leaveRequest.member.organization.id);
    leaveRequest.status = request.status();
    leaveRequest.decisionNote = request.decisionNote();
    leaveRequest.decidedAt = Instant.now();
    leaveRequest.decidedByUser = requireUser(permissions.currentActor().userId());
    if (request.status() == LeaveRequestStatus.APPROVED) {
      applyApprovedLeave(leaveRequest);
    }
    return mapper.leaveRequest(leaveRequest);
  }

  @Transactional
  public void materializeApprovedLeavesForSession(AttendanceSession session) {
    if (session.status == AttendanceSessionStatus.CANCELLED) {
      return;
    }
    LocalDate date = sessionDate(session);
    if (date == null) {
      return;
    }
    leaveRequests.findByMember_Organization_IdAndRequestTypeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            session.organization.id,
            LeaveRequestType.LEAVE_LONG_TERM,
            LeaveRequestStatus.APPROVED
        )
        .stream()
        .filter(request -> request.fromDate != null && request.toDate != null)
        .filter(request -> !date.isBefore(request.fromDate) && !date.isAfter(request.toDate))
        .forEach(request -> applyAttendanceRecord(request, session, approvedStatus(request.requestType)));
  }

  @Transactional
  public int expirePendingRequests() {
    Instant now = Instant.now();
    int expired = 0;
    for (AttendanceLeaveRequest request : leaveRequests.findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(LeaveRequestStatus.PENDING)) {
      if (!shouldExpire(request, now)) {
        continue;
      }
      request.status = LeaveRequestStatus.EXPIRED_AUTO_ABSENT;
      if (request.requestType == LeaveRequestType.LEAVE_LONG_TERM) {
        for (AttendanceSession session : sessionsInRange(request.member.organization.id, request.fromDate, request.toDate)) {
          applyAttendanceRecord(request, session, AttendanceRecordStatus.ABSENT);
        }
      } else if (request.session != null) {
        applyAttendanceRecord(request, request.session, AttendanceRecordStatus.ABSENT);
      }
      expired += 1;
    }
    return expired;
  }

  private AttendanceLeaveRequest createSessionBasedRequest(CurrentActor actor, LeaveRequestCreateRequest request) {
    if (request.sessionId() == null) {
      throw new BadRequestException("sessionId is required for session-based leave requests");
    }
    AttendanceSession session = sessions.findByIdAndDeletedAtIsNull(request.sessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found: " + request.sessionId()));
    if (session.status == AttendanceSessionStatus.CANCELLED) {
      throw new BusinessConflictException("Cannot request leave for a cancelled session");
    }
    OrganizationMember member = resolveMembership(actor.userId(), session.organization.id);
    permissions.requireSelfOrClubManage(session.organization.id, actor.userId());
    AttendanceLeaveRequest leaveRequest = leaveRequests.findBySession_IdAndMember_IdAndDeletedAtIsNull(session.id, member.id)
        .orElseGet(AttendanceLeaveRequest::create);
    if (leaveRequest.id != null) {
      if (leaveRequest.requestType != request.requestType()) {
        throw new BusinessConflictException("A leave request already exists for this session");
      }
      if (leaveRequest.status != LeaveRequestStatus.PENDING) {
        throw new BusinessConflictException("Leave request was already decided");
      }
    }
    populateRequest(leaveRequest, member, actor.userId(), request);
    leaveRequest.session = session;
    leaveRequest.fromDate = null;
    leaveRequest.toDate = null;
    leaveRequest.expiresAt = session.scheduledAt;
    return leaveRequests.save(leaveRequest);
  }

  private AttendanceLeaveRequest createLongTermRequest(CurrentActor actor, LeaveRequestCreateRequest request) {
    if (request.fromDate() == null || request.toDate() == null) {
      throw new BadRequestException("fromDate and toDate are required for long-term leave requests");
    }
    if (request.toDate().isBefore(request.fromDate())) {
      throw new BadRequestException("toDate must be on or after fromDate");
    }
    OrganizationMember member = resolveMembership(actor.userId(), actor.primaryOrganizationId());
    permissions.requireSelfOrClubManage(member.organization.id, actor.userId());
    AttendanceLeaveRequest leaveRequest = AttendanceLeaveRequest.create();
    populateRequest(leaveRequest, member, actor.userId(), request);
    leaveRequest.session = null;
    leaveRequest.fromDate = request.fromDate();
    leaveRequest.toDate = request.toDate();
    leaveRequest.expiresAt = request.toDate().atTime(LocalTime.MAX).atZone(CLUB_ZONE).toInstant();
    return leaveRequests.save(leaveRequest);
  }

  private void populateRequest(
      AttendanceLeaveRequest leaveRequest,
      OrganizationMember member,
      UUID actorUserId,
      LeaveRequestCreateRequest request
  ) {
    leaveRequest.member = member;
    leaveRequest.requesterUser = requireUser(actorUserId);
    leaveRequest.requestType = request.requestType();
    leaveRequest.status = LeaveRequestStatus.PENDING;
    leaveRequest.reason = request.reason().trim();
    leaveRequest.decisionNote = null;
    leaveRequest.decidedAt = null;
    leaveRequest.decidedByUser = null;
  }

  private OrganizationMember resolveMembership(UUID userId, UUID preferredOrganizationId) {
    List<OrganizationMember> memberships = members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    if (preferredOrganizationId != null) {
      return memberships.stream()
          .filter(member -> member.organization.id.equals(preferredOrganizationId))
          .findFirst()
          .orElseThrow(() -> new ResourceNotFoundException("Current user is not a member of this organization"));
    }
    if (memberships.size() == 1) {
      return memberships.get(0);
    }
    throw new BadRequestException("A primary organization is required for this leave request");
  }

  private void applyApprovedLeave(AttendanceLeaveRequest leaveRequest) {
    if (leaveRequest.requestType == LeaveRequestType.LEAVE_LONG_TERM) {
      for (AttendanceSession session : sessionsInRange(leaveRequest.member.organization.id, leaveRequest.fromDate, leaveRequest.toDate)) {
        applyAttendanceRecord(leaveRequest, session, approvedStatus(leaveRequest.requestType));
      }
      return;
    }
    if (leaveRequest.session != null) {
      applyAttendanceRecord(leaveRequest, leaveRequest.session, approvedStatus(leaveRequest.requestType));
    }
  }

  private AttendanceRecordStatus approvedStatus(LeaveRequestType requestType) {
    return requestType == LeaveRequestType.LATE ? AttendanceRecordStatus.LATE : AttendanceRecordStatus.EXCUSED;
  }

  private List<AttendanceSession> sessionsInRange(UUID organizationId, LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      return List.of();
    }
    return sessions.findByOrganization_IdAndScheduledDateBetweenAndDeletedAtIsNullOrderByScheduledAtAscCreatedAtAsc(
            organizationId,
            from,
            to
        )
        .stream()
        .filter(session -> session.status != AttendanceSessionStatus.CANCELLED)
        .toList();
  }

  private void applyAttendanceRecord(AttendanceLeaveRequest leaveRequest, AttendanceSession session, AttendanceRecordStatus desiredStatus) {
    AttendanceRecord record = existingRecord(session, leaveRequest.member).orElseGet(AttendanceRecord::create);
    if (record.id != null && !canReplace(record.status)) {
      return;
    }
    record.session = session;
    record.organizationMember = leaveRequest.member;
    record.athlete = resolveAthlete(session.organization.id, leaveRequest.member);
    record.status = desiredStatus;
    record.note = noteFor(leaveRequest, desiredStatus);
    records.save(record);
  }

  private java.util.Optional<AttendanceRecord> existingRecord(AttendanceSession session, OrganizationMember member) {
    java.util.Optional<AttendanceRecord> byMember = records.findBySession_IdAndOrganizationMember_IdAndDeletedAtIsNull(session.id, member.id);
    if (byMember.isPresent()) {
      return byMember;
    }
    Athlete athlete = resolveAthlete(session.organization.id, member);
    if (athlete == null) {
      return java.util.Optional.empty();
    }
    return records.findBySession_IdAndAthlete_IdAndDeletedAtIsNull(session.id, athlete.id);
  }

  private Athlete resolveAthlete(UUID organizationId, OrganizationMember member) {
    if (member.person == null) {
      return null;
    }
    return athletes.findByPerson_IdAndDeletedAtIsNull(member.person.id)
        .filter(athlete -> roster.findByOrganization_IdAndAthlete_IdAndStatusAndDeletedAtIsNull(
            organizationId,
            athlete.id,
            ClubRosterStatus.ACTIVE
        ).isPresent())
        .orElse(null);
  }

  private boolean canReplace(AttendanceRecordStatus existingStatus) {
    return existingStatus == null || existingStatus == AttendanceRecordStatus.ABSENT;
  }

  private boolean shouldExpire(AttendanceLeaveRequest request, Instant now) {
    if (request.expiresAt != null) {
      return !request.expiresAt.isAfter(now);
    }
    if (request.session != null && request.session.scheduledAt != null) {
      return !request.session.scheduledAt.isAfter(now);
    }
    if (request.toDate != null) {
      return !request.toDate.atTime(LocalTime.MAX).atZone(CLUB_ZONE).toInstant().isAfter(now);
    }
    return false;
  }

  private LocalDate sessionDate(AttendanceSession session) {
    if (session.scheduledDate != null) {
      return session.scheduledDate;
    }
    if (session.scheduledAt != null) {
      return session.scheduledAt.atZone(CLUB_ZONE).toLocalDate();
    }
    return null;
  }

  private String noteFor(AttendanceLeaveRequest request, AttendanceRecordStatus status) {
    String prefix = switch (status) {
      case EXCUSED -> "Leave approved";
      case LATE -> "Late approved";
      case ABSENT -> "Leave expired";
      default -> "Attendance updated";
    };
    return prefix + ": " + request.reason;
  }

  private AttendanceLeaveRequest requireLeaveRequest(UUID requestId) {
    return leaveRequests.findByIdAndDeletedAtIsNull(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + requestId));
  }

  private AppUser requireUser(UUID userId) {
    return users.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
  }
}
