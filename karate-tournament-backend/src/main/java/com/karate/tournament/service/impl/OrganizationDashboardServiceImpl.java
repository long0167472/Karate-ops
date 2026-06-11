package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.ClubRoster;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.repository.AthleteRepository;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.AttendanceSessionRepository;
import com.karate.tournament.repository.ClubRosterRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.dto.response.LowAttendanceAthleteRow;
import com.karate.tournament.dto.response.OrganizationAttendanceDashboardResponse;
import com.karate.tournament.dto.response.OrganizationAthleteDashboardResponse;
import com.karate.tournament.dto.response.OrganizationDashboardOverviewResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationDashboardServiceImpl implements OrganizationDashboardService {
  private final OrganizationRepository organizations;
  private final ClubRosterRepository roster;
  private final AttendanceSessionRepository sessions;
  private final AttendanceRecordRepository records;
  private final AthleteRepository athletes;
  private final EntryRepository entries;
  private final OrganizationMemberRepository members;
  private final PermissionService permissions;

  @Transactional(readOnly = true)
  public OrganizationDashboardOverviewResponse overview(UUID organizationId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    List<AttendanceSession> sessionList = sessions.findByOrganization_IdAndDeletedAtIsNullOrderByScheduledAtDescCreatedAtDesc(organizationId);
    AttendanceCounts counts = attendanceCounts(sessionList);
    return new OrganizationDashboardOverviewResponse(
        organization.id,
        organization.name,
        members.countByOrganization_IdAndStatusAndDeletedAtIsNull(organization.id, ClubMemberStatus.ACTIVE),
        roster.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organization.id)
            .stream()
            .filter(row -> row.status == ClubRosterStatus.ACTIVE)
            .count(),
        sessionList.size(),
        tournamentEntries(organization.id, null),
        rate(counts.present() + counts.late(), counts.present() + counts.late() + counts.absent())
    );
  }

  @Transactional(readOnly = true)
  public OrganizationAttendanceDashboardResponse attendance(UUID organizationId, LocalDate from, LocalDate to) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    LocalDate safeTo = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
    LocalDate safeFrom = from == null ? safeTo.withDayOfMonth(1) : from;
    List<AttendanceSession> sessionList = sessions.findByOrganization_IdAndDeletedAtIsNullOrderByScheduledAtDescCreatedAtDesc(organizationId)
        .stream()
        .filter(session -> inRange(session, safeFrom, safeTo))
        .toList();
    AttendanceCounts counts = attendanceCounts(sessionList);
    return new OrganizationAttendanceDashboardResponse(
        organization.id,
        safeFrom,
        safeTo,
        sessionList.size(),
        counts.total(),
        counts.present(),
        counts.absent(),
        counts.late(),
        counts.excused(),
        rate(counts.present() + counts.late(), counts.present() + counts.late() + counts.absent()),
        lowAttendanceRows(organization.id, sessionList)
    );
  }

  @Transactional(readOnly = true)
  public OrganizationAthleteDashboardResponse athlete(UUID organizationId, UUID athleteId) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    Athlete athlete = athletes.findByIdAndDeletedAtIsNull(athleteId)
        .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + athleteId));
    roster.findByOrganization_IdAndAthlete_IdAndDeletedAtIsNull(organization.id, athlete.id)
        .orElseThrow(() -> new ResourceNotFoundException("Athlete is not in this organization roster"));
    List<AttendanceSession> sessionList = sessions.findByOrganization_IdAndDeletedAtIsNullOrderByScheduledAtDescCreatedAtDesc(organization.id);
    List<AttendanceRecord> athleteRecords = recordsForAthlete(sessionList, athlete.id);
    AttendanceCounts counts = countRecords(athleteRecords);
    return new OrganizationAthleteDashboardResponse(
        organization.id,
        athlete.id,
        athlete.person.displayName,
        sessionList.size(),
        counts.present(),
        counts.late(),
        counts.absent(),
        counts.excused(),
        rate(counts.present() + counts.late(), counts.present() + counts.late() + counts.absent()),
        tournamentEntries(organization.id, athlete.id),
        tournamentNames(organization.id, athlete.id)
    );
  }

  private List<LowAttendanceAthleteRow> lowAttendanceRows(UUID organizationId, List<AttendanceSession> sessionList) {
    return roster.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .filter(row -> row.status == ClubRosterStatus.ACTIVE)
        .map(row -> lowAttendanceRow(row, sessionList))
        .sorted(Comparator.comparingDouble(LowAttendanceAthleteRow::attendanceRate)
            .thenComparing(LowAttendanceAthleteRow::athleteName))
        .limit(10)
        .toList();
  }

  private LowAttendanceAthleteRow lowAttendanceRow(ClubRoster row, List<AttendanceSession> sessionList) {
    List<AttendanceRecord> athleteRecords = recordsForAthlete(sessionList, row.athlete.id);
    AttendanceCounts counts = countRecords(athleteRecords);
    long presentOrLate = counts.present() + counts.late();
    long denominator = presentOrLate + counts.absent();
    return new LowAttendanceAthleteRow(
        row.athlete.id,
        row.athlete.person.displayName,
        sessionList.size(),
        presentOrLate,
        rate(presentOrLate, denominator)
    );
  }

  private AttendanceCounts attendanceCounts(List<AttendanceSession> sessionList) {
    List<AttendanceRecord> all = new ArrayList<>();
    for (AttendanceSession session : sessionList) {
      all.addAll(records.findBySession_IdAndDeletedAtIsNullOrderByCreatedAtAsc(session.id));
    }
    return countRecords(all);
  }

  private AttendanceCounts countRecords(List<AttendanceRecord> recordList) {
    long present = 0;
    long absent = 0;
    long late = 0;
    long excused = 0;
    for (AttendanceRecord record : recordList) {
      if (record.status == AttendanceRecordStatus.PRESENT) present += 1;
      if (record.status == AttendanceRecordStatus.ABSENT) absent += 1;
      if (record.status == AttendanceRecordStatus.LATE) late += 1;
      if (record.status == AttendanceRecordStatus.EXCUSED) excused += 1;
    }
    return new AttendanceCounts(recordList.size(), present, absent, late, excused);
  }

  private List<AttendanceRecord> recordsForAthlete(List<AttendanceSession> sessionList, UUID athleteId) {
    List<AttendanceRecord> out = new ArrayList<>();
    for (AttendanceSession session : sessionList) {
      records.findBySession_IdAndAthlete_IdAndDeletedAtIsNull(session.id, athleteId).ifPresent(out::add);
    }
    return out;
  }

  private boolean inRange(AttendanceSession session, LocalDate from, LocalDate to) {
    if (session.scheduledAt == null) {
      return false;
    }
    LocalDate date = session.scheduledAt.atZone(ZoneOffset.UTC).toLocalDate();
    return !date.isBefore(from) && !date.isAfter(to);
  }

  private long tournamentEntries(UUID organizationId, UUID athleteId) {
    if (athleteId == null) {
      return entries.countTournamentEntriesByOrganization(organizationId);
    }
    return entries.countTournamentEntriesByOrganizationAndAthlete(organizationId, athleteId);
  }

  private List<String> tournamentNames(UUID organizationId, UUID athleteId) {
    return entries.findTournamentNamesByOrganizationAndAthlete(organizationId, athleteId);
  }

  private double rate(long numerator, long denominator) {
    if (denominator <= 0) {
      return 0;
    }
    return Math.round((double) numerator * 10000.0 / (double) denominator) / 100.0;
  }

  private Organization requireOrganization(UUID id) {
    return organizations.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
  }

  private record AttendanceCounts(long total, long present, long absent, long late, long excused) {
  }
}
