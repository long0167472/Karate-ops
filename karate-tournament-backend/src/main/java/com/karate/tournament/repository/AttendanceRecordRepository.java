package com.karate.tournament.repository;

import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
  List<AttendanceRecord> findBySession_IdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID sessionId);

  List<AttendanceRecord> findBySession_IdInAndDeletedAtIsNullOrderByCreatedAtAsc(List<UUID> sessionIds);

  List<AttendanceRecord> findByOrganizationMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

  Optional<AttendanceRecord> findByIdAndDeletedAtIsNull(UUID id);

  Optional<AttendanceRecord> findBySession_IdAndOrganizationMember_IdAndDeletedAtIsNull(UUID sessionId, UUID memberId);

  Optional<AttendanceRecord> findBySession_IdAndAthlete_IdAndDeletedAtIsNull(UUID sessionId, UUID athleteId);

  boolean existsBySession_IdAndDeletedAtIsNull(UUID sessionId);

  @Query("""
      select r.session.organization.id as organizationId,
             sum(case when r.status in :presentStatuses then 1 else 0 end) as presentOrLate,
             sum(case when r.status = :absentStatus then 1 else 0 end) as absent
      from AttendanceRecord r
      where r.session.organization.id in :organizationIds
        and r.deletedAt is null
        and r.session.deletedAt is null
      group by r.session.organization.id
      """)
  List<AttendanceRateProjection> summarizeAttendanceByOrganizationIds(
      @Param("organizationIds") List<UUID> organizationIds,
      @Param("presentStatuses") List<AttendanceRecordStatus> presentStatuses,
      @Param("absentStatus") AttendanceRecordStatus absentStatus
  );

  interface AttendanceRateProjection {
    UUID getOrganizationId();

    Long getPresentOrLate();

    Long getAbsent();
  }
}
