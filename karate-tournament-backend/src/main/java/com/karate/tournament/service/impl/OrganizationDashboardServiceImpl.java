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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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
    return buildOverviewMap(List.of(organization)).get(organization.id);
  }

  @Transactional(readOnly = true)
  public Map<UUID, OrganizationDashboardOverviewResponse> overviews(List<Organization> organizationsList) {
    organizationsList.forEach(organization -> permissions.requireClubView(organization.id));
    return buildOverviewMap(organizationsList);
  }

  @Transactional(readOnly = true)
  public OrganizationAttendanceDashboardResponse attendance(UUID organizationId, LocalDate from, LocalDate to) {
    Organization organization = requireOrganization(organizationId);
    permissions.requireClubView(organization.id);
    LocalDate safeTo = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
    LocalDate safeFrom = from == null ? safeTo.withDayOfMonth(1) : from;
    List<AttendanceSession> sessionList = sessions.findByOrganization_IdAndScheduledDateBetweenAndDeletedAtIsNullOrderByScheduledAtAscCreatedAtAsc(
        organizationId,
        safeFrom,
        safeTo
    );
    List<AttendanceRecord> sessionRecords = recordsForSessions(sessionList);
    AttendanceCounts counts = countRecords(sessionRecords);
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
        lowAttendanceRows(organization.id, sessionList, sessionRecords)
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
    List<AttendanceRecord> athleteRecords = recordsForSessions(sessionList).stream()
        .filter(record -> record.athlete != null && record.athlete.id.equals(athlete.id))
        .toList();
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

  private List<LowAttendanceAthleteRow> lowAttendanceRows(UUID organizationId, List<AttendanceSession> sessionList, List<AttendanceRecord> sessionRecords) {
    var recordsByAthlete = sessionRecords.stream()
        .filter(record -> record.athlete != null)
        .collect(java.util.stream.Collectors.groupingBy(record -> record.athlete.id));
    return roster.findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .filter(row -> row.status == ClubRosterStatus.ACTIVE)
        .map(row -> lowAttendanceRow(row, sessionList.size(), recordsByAthlete.getOrDefault(row.athlete.id, List.of())))
        .sorted(Comparator.comparingDouble(LowAttendanceAthleteRow::attendanceRate)
            .thenComparing(LowAttendanceAthleteRow::athleteName))
        .limit(10)
        .toList();
  }

  private LowAttendanceAthleteRow lowAttendanceRow(ClubRoster row, int sessionCount, List<AttendanceRecord> athleteRecords) {
    AttendanceCounts counts = countRecords(athleteRecords);
    long presentOrLate = counts.present() + counts.late();
    long denominator = presentOrLate + counts.absent();
    return new LowAttendanceAthleteRow(
        row.athlete.id,
        row.athlete.person.displayName,
        sessionCount,
        presentOrLate,
        rate(presentOrLate, denominator)
    );
  }

  private List<AttendanceRecord> recordsForSessions(List<AttendanceSession> sessionList) {
    if (sessionList.isEmpty()) {
      return List.of();
    }
    return records.findBySession_IdInAndDeletedAtIsNullOrderByCreatedAtAsc(
        sessionList.stream().map(session -> session.id).toList()
    );
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

  private Map<UUID, OrganizationDashboardOverviewResponse> buildOverviewMap(List<Organization> organizationsList) {
    if (organizationsList.isEmpty()) {
      return Map.of();
    }
    List<UUID> organizationIds = organizationsList.stream().map(organization -> organization.id).toList();
    Map<UUID, Long> activeMembersByOrg = countMap(
        members.countByOrganizationIdsAndStatus(organizationIds, ClubMemberStatus.ACTIVE),
        OrganizationMemberRepository.OrganizationCountProjection::getOrganizationId,
        OrganizationMemberRepository.OrganizationCountProjection::getTotal
    );
    Map<UUID, Long> activeAthletesByOrg = countMap(
        roster.countByOrganizationIdsAndStatus(organizationIds, ClubRosterStatus.ACTIVE),
        ClubRosterRepository.OrganizationCountProjection::getOrganizationId,
        ClubRosterRepository.OrganizationCountProjection::getTotal
    );
    Map<UUID, Long> sessionsByOrg = countMap(
        sessions.countByOrganizationIds(organizationIds),
        AttendanceSessionRepository.OrganizationCountProjection::getOrganizationId,
        AttendanceSessionRepository.OrganizationCountProjection::getTotal
    );
    Map<UUID, Long> entriesByOrg = countMap(
        entries.countTournamentEntriesByOrganizationIds(organizationIds),
        EntryRepository.OrganizationCountProjection::getOrganizationId,
        EntryRepository.OrganizationCountProjection::getTotal
    );
    Map<UUID, AttendanceRateSnapshot> ratesByOrg = attendanceRateMap(organizationIds);

    Map<UUID, OrganizationDashboardOverviewResponse> results = new LinkedHashMap<>();
    for (Organization organization : organizationsList) {
      AttendanceRateSnapshot rateSnapshot = ratesByOrg.getOrDefault(organization.id, AttendanceRateSnapshot.ZERO);
      results.put(
          organization.id,
          new OrganizationDashboardOverviewResponse(
              organization.id,
              organization.name,
              activeMembersByOrg.getOrDefault(organization.id, 0L),
              activeAthletesByOrg.getOrDefault(organization.id, 0L),
              sessionsByOrg.getOrDefault(organization.id, 0L),
              entriesByOrg.getOrDefault(organization.id, 0L),
              rate(rateSnapshot.presentOrLate(), rateSnapshot.presentOrLate() + rateSnapshot.absent())
          )
      );
    }
    return results;
  }

  private <T> Map<UUID, Long> countMap(List<T> rows, Function<T, UUID> organizationId, Function<T, Long> total) {
    Map<UUID, Long> counts = new LinkedHashMap<>();
    for (T row : rows) {
      Long value = total.apply(row);
      counts.put(organizationId.apply(row), value == null ? 0L : value);
    }
    return counts;
  }

  private Map<UUID, AttendanceRateSnapshot> attendanceRateMap(List<UUID> organizationIds) {
    Map<UUID, AttendanceRateSnapshot> rates = new LinkedHashMap<>();
    for (AttendanceRecordRepository.AttendanceRateProjection row : records.summarizeAttendanceByOrganizationIds(
        organizationIds,
        List.of(AttendanceRecordStatus.PRESENT, AttendanceRecordStatus.LATE),
        AttendanceRecordStatus.ABSENT
    )) {
      rates.put(
          row.getOrganizationId(),
          new AttendanceRateSnapshot(
              row.getPresentOrLate() == null ? 0L : row.getPresentOrLate(),
              row.getAbsent() == null ? 0L : row.getAbsent()
          )
      );
    }
    return rates;
  }

  private record AttendanceRateSnapshot(long presentOrLate, long absent) {
    private static final AttendanceRateSnapshot ZERO = new AttendanceRateSnapshot(0, 0);
  }
}
