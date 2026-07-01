package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.enums.AttendanceSessionSource;
import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;
import com.karate.tournament.entity.ClubTrainingSchedule;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.AttendanceSessionRepository;
import com.karate.tournament.repository.ClubTrainingScheduleRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.dto.request.ClubTrainingScheduleRequest;
import com.karate.tournament.dto.response.ClubTrainingScheduleResponse;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubTrainingScheduleServiceImpl implements ClubTrainingScheduleService {
  private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final ClubTrainingScheduleRepository schedules;
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final OrganizationRepository organizations;
  private final AttendanceLeaveRequestService leaveRequests;
  private final PermissionService permissions;

  @Transactional(readOnly = true)
  public ClubTrainingScheduleResponse get(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    return schedules.findByOrganization_IdAndDeletedAtIsNull(organizationId)
        .map(this::toResponse)
        .orElseGet(() -> defaultResponse(organization));
  }

  @Transactional
  public ClubTrainingScheduleResponse update(UUID organizationId, ClubTrainingScheduleRequest request) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireAttendanceManage(organization.id);
    ClubTrainingSchedule schedule = schedules.findByOrganization_IdAndDeletedAtIsNull(organizationId)
        .orElseGet(ClubTrainingSchedule::create);
    schedule.organization = organization;
    schedule.name = request.name();
    schedule.daysOfWeek = encodeDays(request.daysOfWeek());
    schedule.startTime = parseTime(request.startTime());
    schedule.durationMinutes = request.durationMinutes() == null ? 90 : request.durationMinutes();
    if (schedule.durationMinutes < 30 || schedule.durationMinutes > 240) {
      throw new BadRequestException("durationMinutes must be between 30 and 240");
    }
    schedule.timezone = request.timezone() == null || request.timezone().isBlank() ? DEFAULT_ZONE.getId() : request.timezone();
    validateZone(schedule.timezone);
    schedule.active = request.active() == null || request.active();
    schedule = schedules.save(schedule);
    reconcileToday(schedule);
    return toResponse(schedule);
  }

  @Transactional
  public int ensureTodaySessions() {
    int created = 0;
    for (ClubTrainingSchedule schedule : schedules.findByActiveTrueAndDeletedAtIsNull()) {
      created += ensureSessionForDate(schedule, LocalDate.now(zone(schedule.timezone)));
    }
    return created;
  }

  @Transactional
  public int ensureTodaySession(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireAttendanceManage(organization.id);
    return schedules.findByOrganization_IdAndDeletedAtIsNull(organizationId)
        .map(schedule -> ensureSessionForDate(schedule, LocalDate.now(zone(schedule.timezone))))
        .orElse(0);
  }

  @Transactional
  public int markDayOff(UUID organizationId, LocalDate date) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireAttendanceManage(organization.id);
    ClubTrainingSchedule schedule = schedules.findByOrganization_IdAndDeletedAtIsNull(organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("Training schedule not found for organization: " + organizationId));
    List<AttendanceSession> existing = sessions.findByTrainingSchedule_IdAndSourceAndScheduledDateBetweenAndDeletedAtIsNull(
        schedule.id,
        AttendanceSessionSource.SCHEDULED,
        date,
        date
    );
    if (!existing.isEmpty()) {
      for (AttendanceSession session : existing) {
        session.status = AttendanceSessionStatus.CANCELLED;
        session.notes = "CLB đánh dấu nghỉ tập ngày này.";
      }
      return 0;
    }
    AttendanceSession session = AttendanceSession.create();
    session.organization = organization;
    session.name = "Nghỉ tập - " + date;
    session.type = AttendanceSessionType.TRAINING;
    session.status = AttendanceSessionStatus.CANCELLED;
    session.source = AttendanceSessionSource.SCHEDULED;
    session.trainingSchedule = schedule;
    session.scheduledDate = date;
    session.scheduledAt = ZonedDateTime.of(date, schedule.startTime, zone(schedule.timezone)).toInstant();
    session.notes = "CLB đánh dấu nghỉ tập ngày này.";
    AttendanceSession saved = sessions.save(session);
    leaveRequests.materializeApprovedLeavesForSession(saved);
    return 1;
  }

  private void reconcileToday(ClubTrainingSchedule schedule) {
    LocalDate today = LocalDate.now(zone(schedule.timezone));
    if (schedule.active && days(schedule.daysOfWeek).contains(today.getDayOfWeek().getValue())) {
      ensureSessionForDate(schedule, today);
      return;
    }
    sessions.findByTrainingSchedule_IdAndSourceAndScheduledDateBetweenAndDeletedAtIsNull(
            schedule.id,
            AttendanceSessionSource.SCHEDULED,
            today,
            today
        )
        .stream()
        .filter(session -> !records.existsBySession_IdAndDeletedAtIsNull(session.id))
        .forEach(AttendanceSession::softDelete);
  }

  private int ensureSessionForDate(ClubTrainingSchedule schedule, LocalDate date) {
    if (!schedule.active || !days(schedule.daysOfWeek).contains(date.getDayOfWeek().getValue())) {
      return 0;
    }
    List<AttendanceSession> existing = sessions.findByTrainingSchedule_IdAndSourceAndScheduledDateBetweenAndDeletedAtIsNull(
        schedule.id,
        AttendanceSessionSource.SCHEDULED,
        date,
        date
    );
    if (!existing.isEmpty()) {
      return 0;
    }
    AttendanceSession session = AttendanceSession.create();
    session.organization = schedule.organization;
    session.name = schedule.name + " - " + date;
    session.type = AttendanceSessionType.TRAINING;
    session.status = AttendanceSessionStatus.OPEN;
    session.source = AttendanceSessionSource.SCHEDULED;
    session.trainingSchedule = schedule;
    session.scheduledDate = date;
    session.scheduledAt = ZonedDateTime.of(date, schedule.startTime, zone(schedule.timezone)).toInstant();
    session.notes = "Tự tạo từ lịch tập cố định.";
    AttendanceSession saved = sessions.save(session);
    leaveRequests.materializeApprovedLeavesForSession(saved);
    return 1;
  }

  private ClubTrainingScheduleResponse toResponse(ClubTrainingSchedule schedule) {
    return new ClubTrainingScheduleResponse(
        schedule.id,
        schedule.organization.id,
        schedule.organization.name,
        schedule.name,
        days(schedule.daysOfWeek),
        schedule.startTime.toString(),
        schedule.durationMinutes,
        schedule.timezone,
        schedule.active
    );
  }

  private ClubTrainingScheduleResponse defaultResponse(Organization organization) {
    return new ClubTrainingScheduleResponse(
        null,
        organization.id,
        organization.name,
        "Lịch tập chính " + (organization.shortName == null || organization.shortName.isBlank() ? organization.name : organization.shortName),
        List.of(),
        "18:30",
        90,
        DEFAULT_ZONE.getId(),
        true
    );
  }

  private String encodeDays(List<Integer> values) {
    if (values == null) return "";
    Set<Integer> unique = new HashSet<>();
    for (Integer value : values) {
      if (value == null || value < 1 || value > 7) {
        throw new BadRequestException("daysOfWeek must contain ISO values 1..7");
      }
      unique.add(value);
    }
    return unique.stream().sorted().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
  }

  private List<Integer> days(String encoded) {
    if (encoded == null || encoded.isBlank()) return List.of();
    return java.util.Arrays.stream(encoded.split(","))
        .filter(value -> !value.isBlank())
        .map(Integer::parseInt)
        .sorted()
        .toList();
  }

  private LocalTime parseTime(String value) {
    if (value == null || value.isBlank()) return LocalTime.of(18, 30);
    try {
      return LocalTime.parse(value);
    } catch (DateTimeException ex) {
      throw new BadRequestException("startTime must use HH:mm format");
    }
  }

  private ZoneId zone(String value) {
    return value == null || value.isBlank() ? DEFAULT_ZONE : ZoneId.of(value);
  }

  private void validateZone(String value) {
    zone(value);
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }
}
