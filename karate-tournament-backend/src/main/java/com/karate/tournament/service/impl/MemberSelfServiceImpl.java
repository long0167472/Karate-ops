package com.karate.tournament.service.impl;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.response.AttendanceRecordResponse;
import com.karate.tournament.dto.response.MemberAttendanceSessionResponse;
import com.karate.tournament.dto.response.MemberAttendanceSummaryResponse;
import com.karate.tournament.dto.response.MemberClubProfileResponse;
import com.karate.tournament.dto.response.MemberFeeAssignmentResponse;
import com.karate.tournament.dto.response.MemberFeeSummaryResponse;
import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.MemberFeeAssignment;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AttendanceLeaveRequestRepository;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.AttendanceSessionRepository;
import com.karate.tournament.repository.MemberFeeAssignmentRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.service.MemberSelfService;
import com.karate.tournament.web.ApiMapper;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberSelfServiceImpl implements MemberSelfService {
  private final OrganizationMemberRepository members;
  private final MemberFeeAssignmentRepository assignments;
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final AttendanceLeaveRequestRepository leaveRequests;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public MemberClubProfileResponse clubProfile() {
    return new MemberClubProfileResponse(myMemberships().stream().map(mapper::clubMember).toList());
  }

  @Transactional(readOnly = true)
  public MemberFeeSummaryResponse fees() {
    List<MemberFeeAssignment> rows = myMemberships().stream()
        .flatMap(member -> assignments.findByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(member.id).stream())
        .toList();
    BigDecimal totalDue = rows.stream().map(row -> row.amountDue).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPaid = rows.stream().map(row -> row.paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal remaining = rows.stream()
        .map(row -> row.amountDue.subtract(row.paidAmount).max(BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new MemberFeeSummaryResponse(totalDue, totalPaid, remaining, rows.stream().map(this::assignmentResponse).toList());
  }

  @Transactional(readOnly = true)
  public MemberAttendanceSummaryResponse attendance() {
    List<OrganizationMember> myMembers = myMemberships();
    Map<UUID, OrganizationMember> membersByOrg = myMembers.stream()
        .collect(Collectors.toMap(member -> member.organization.id, Function.identity(), (left, right) -> left));
    List<AttendanceSession> sessionRows = myMembers.stream()
        .flatMap(member -> sessions.findByOrganization_IdAndDeletedAtIsNullOrderByScheduledAtDescCreatedAtDesc(member.organization.id).stream())
        .distinct()
        .sorted(Comparator.comparing((AttendanceSession session) -> session.scheduledAt == null ? java.time.Instant.EPOCH : session.scheduledAt).reversed())
        .toList();
    List<AttendanceRecord> recordRows = myMembers.stream()
        .flatMap(member -> records.findByOrganizationMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(member.id).stream())
        .toList();
    Map<UUID, AttendanceRecord> recordBySession = recordRows.stream()
        .collect(Collectors.toMap(record -> record.session.id, Function.identity(), (left, right) -> left));
    var leaveBySession = myMembers.stream()
        .flatMap(member -> leaveRequests.findByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(member.id).stream())
        .collect(Collectors.toMap(request -> request.session.id, Function.identity(), (left, right) -> left));
    List<MemberAttendanceSessionResponse> responseRows = sessionRows.stream()
        .map(session -> {
          OrganizationMember member = membersByOrg.get(session.organization.id);
          AttendanceRecord record = recordBySession.get(session.id);
          return new MemberAttendanceSessionResponse(
              session.id,
              session.organization.id,
              session.organization.name,
              session.name,
              session.type,
              session.status,
              session.scheduledAt,
              session.scheduledDate,
              record == null ? null : mapper.attendanceRecord(record),
              leaveBySession.containsKey(session.id) ? mapper.leaveRequest(leaveBySession.get(session.id)) : null
          );
        })
        .toList();
    return new MemberAttendanceSummaryResponse(
        responseRows.size(),
        count(recordRows, AttendanceRecordStatus.PRESENT),
        count(recordRows, AttendanceRecordStatus.LATE),
        count(recordRows, AttendanceRecordStatus.ABSENT),
        count(recordRows, AttendanceRecordStatus.EXCUSED),
        leaveBySession.values().stream().filter(request -> request.status == LeaveRequestStatus.PENDING).count(),
        responseRows
    );
  }

  private List<OrganizationMember> myMemberships() {
    CurrentActor actor = permissions.currentActor();
    permissions.requireMemberSelfView(actor.userId());
    return members.findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId());
  }

  private long count(List<AttendanceRecord> rows, AttendanceRecordStatus status) {
    return rows.stream().filter(record -> record.status == status).count();
  }

  private MemberFeeAssignmentResponse assignmentResponse(MemberFeeAssignment assignment) {
    return new MemberFeeAssignmentResponse(
        assignment.id,
        assignment.organization.id,
        assignment.member.id,
        assignment.member.person == null ? assignment.member.user == null ? null : assignment.member.user.displayName : assignment.member.person.displayName,
        assignment.feeItem.id,
        assignment.feeItem.name,
        assignment.assignedRole == null ? null : assignment.assignedRole.id,
        assignment.assignedRole == null ? null : assignment.assignedRole.name,
        assignment.amountDue,
        assignment.paidAmount,
        assignment.status,
        assignment.dueDate,
        assignment.source,
        assignment.note
    );
  }
}
