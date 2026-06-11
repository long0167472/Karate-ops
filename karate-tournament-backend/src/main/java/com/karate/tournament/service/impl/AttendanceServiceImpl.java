package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.enums.AttendanceSessionSource;
import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.AttendanceSessionRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.AttendanceRecordRequest;
import com.karate.tournament.dto.response.AttendanceRecordResponse;
import com.karate.tournament.dto.request.AttendanceRecordUpdateRequest;
import com.karate.tournament.dto.request.AttendanceSessionCreateRequest;
import com.karate.tournament.dto.response.AttendanceSessionResponse;
import com.karate.tournament.dto.request.AttendanceSessionUpdateRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final OrganizationRepository organizations;
  private final TournamentParticipantRepository tournamentParticipants;
  private final OrganizationMemberRepository members;
  private final AthleteRepository athletes;
  private final ClubRosterService rosterService;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<AttendanceSessionResponse> list(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    return sessions.findByOrganization_IdAndDeletedAtIsNullOrderByScheduledAtDescCreatedAtDesc(organizationId)
        .stream()
        .map(mapper::attendanceSession)
        .toList();
  }

  @Transactional(readOnly = true)
  public AttendanceSessionResponse get(UUID sessionId) {
    AttendanceSession session = requireSession(sessionId);
    permissions.requireClubView(session.organization.id);
    return mapper.attendanceSession(session);
  }

  @Transactional
  public AttendanceSessionResponse create(UUID organizationId, AttendanceSessionCreateRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireAttendanceManage(organization.id);
    AttendanceSession session = AttendanceSession.create();
    session.organization = organization;
    session.name = request.name();
    session.type = request.type() == null ? AttendanceSessionType.TRAINING : request.type();
    session.status = request.status() == null ? AttendanceSessionStatus.OPEN : request.status();
    session.scheduledAt = request.scheduledAt() == null ? Instant.now() : request.scheduledAt();
    session.scheduledDate = session.scheduledAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
    session.source = AttendanceSessionSource.MANUAL;
    session.notes = request.notes();
    if (request.tournamentParticipantId() != null) {
      TournamentParticipant participant = tournamentParticipants.findByIdAndDeletedAtIsNull(request.tournamentParticipantId())
          .orElseThrow(() -> new ResourceNotFoundException("Tournament participant not found: " + request.tournamentParticipantId()));
      if (!participant.organization.id.equals(organization.id)) {
        throw new BadRequestException("Tournament participant belongs to another organization");
      }
      session.tournamentParticipant = participant;
    }
    return mapper.attendanceSession(sessions.save(session));
  }

  @Transactional
  public AttendanceSessionResponse update(UUID sessionId, AttendanceSessionUpdateRequest request) {
    AttendanceSession session = requireSession(sessionId);
    permissions.requireAttendanceManage(session.organization.id);
    if (request.name() != null) session.name = request.name();
    if (request.type() != null) session.type = request.type();
    if (request.status() != null) session.status = request.status();
    if (request.scheduledAt() != null) {
      session.scheduledAt = request.scheduledAt();
      session.scheduledDate = request.scheduledAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
    }
    if (request.notes() != null) session.notes = request.notes();
    if (request.tournamentParticipantId() != null) {
      TournamentParticipant participant = tournamentParticipants.findByIdAndDeletedAtIsNull(request.tournamentParticipantId())
          .orElseThrow(() -> new ResourceNotFoundException("Tournament participant not found: " + request.tournamentParticipantId()));
      if (!participant.organization.id.equals(session.organization.id)) {
        throw new BadRequestException("Tournament participant belongs to another organization");
      }
      session.tournamentParticipant = participant;
    }
    return mapper.attendanceSession(session);
  }

  @Transactional
  public void deleteManualSession(UUID sessionId) {
    AttendanceSession session = requireSession(sessionId);
    permissions.requireAttendanceManage(session.organization.id);
    if (session.source != AttendanceSessionSource.MANUAL) {
      throw new BusinessConflictException("Only manually added attendance sessions can be deleted");
    }
    session.softDelete();
  }

  @Transactional
  public AttendanceRecordResponse record(UUID sessionId, AttendanceRecordRequest request) {
    AttendanceSession session = requireSession(sessionId);
    permissions.requireAttendanceManage(session.organization.id);
    if (session.status == AttendanceSessionStatus.CANCELLED) {
      throw new BusinessConflictException("Cannot add attendance records to a cancelled session");
    }
    ResolvedAttendanceSubject subject = resolveSubject(session.organization.id, request.organizationMemberId(), request.athleteId());
    AttendanceRecord record = findExistingRecord(session.id, subject)
        .orElseGet(AttendanceRecord::create);
    record.session = session;
    record.organizationMember = subject.member();
    record.athlete = subject.athlete();
    record.status = request.status();
    record.checkInAt = request.checkInAt() == null && isPresentLike(request.status()) ? Instant.now() : request.checkInAt();
    record.note = request.note();
    return mapper.attendanceRecord(records.save(record));
  }

  @Transactional
  public AttendanceRecordResponse updateRecord(UUID sessionId, UUID recordId, AttendanceRecordUpdateRequest request) {
    AttendanceSession session = requireSession(sessionId);
    permissions.requireAttendanceManage(session.organization.id);
    AttendanceRecord record = records.findByIdAndDeletedAtIsNull(recordId)
        .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + recordId));
    if (!record.session.id.equals(sessionId)) {
      throw new ResourceNotFoundException("Attendance record does not belong to session");
    }
    if (request.status() != null) record.status = request.status();
    if (request.checkInAt() != null) record.checkInAt = request.checkInAt();
    if (request.note() != null) record.note = request.note();
    return mapper.attendanceRecord(record);
  }

  public AttendanceSession requireSession(UUID sessionId) {
    return sessions.findByIdAndDeletedAtIsNull(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Attendance session not found: " + sessionId));
  }

  private ResolvedAttendanceSubject resolveSubject(UUID organizationId, UUID memberId, UUID athleteId) {
    if (memberId == null && athleteId == null) {
      throw new BadRequestException("organizationMemberId or athleteId is required");
    }
    OrganizationMember member = null;
    if (memberId != null) {
      member = members.findByIdAndDeletedAtIsNull(memberId)
          .orElseThrow(() -> new ResourceNotFoundException("Organization member not found: " + memberId));
      if (!member.organization.id.equals(organizationId)) {
        throw new BadRequestException("Member belongs to another organization");
      }
    }
    Athlete athlete = null;
    if (athleteId != null) {
      athlete = athletes.findByIdAndDeletedAtIsNull(athleteId)
          .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + athleteId));
      rosterService.requireAthleteBelongsToOrganization(organizationId, athlete);
      if (member != null && member.person != null && !member.person.id.equals(athlete.person.id)) {
        throw new BadRequestException("organizationMemberId and athleteId must refer to the same person");
      }
      if (member == null) {
        member = members.findByOrganization_IdAndPerson_IdAndStatusAndDeletedAtIsNull(
            organizationId,
            athlete.person.id,
            ClubMemberStatus.ACTIVE
        ).orElse(null);
      }
    }
    return new ResolvedAttendanceSubject(member, athlete);
  }

  private java.util.Optional<AttendanceRecord> findExistingRecord(UUID sessionId, ResolvedAttendanceSubject subject) {
    if (subject.member() != null) {
      return records.findBySession_IdAndOrganizationMember_IdAndDeletedAtIsNull(sessionId, subject.member().id);
    }
    if (subject.athlete() != null) {
      return records.findBySession_IdAndAthlete_IdAndDeletedAtIsNull(sessionId, subject.athlete().id);
    }
    return java.util.Optional.empty();
  }

  private boolean isPresentLike(AttendanceRecordStatus status) {
    return status == AttendanceRecordStatus.PRESENT || status == AttendanceRecordStatus.LATE;
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }

  private record ResolvedAttendanceSubject(OrganizationMember member, Athlete athlete) {
  }
}
