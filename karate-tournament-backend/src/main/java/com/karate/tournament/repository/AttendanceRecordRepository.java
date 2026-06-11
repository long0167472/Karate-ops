package com.karate.tournament.repository;

import com.karate.tournament.entity.AttendanceRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
  List<AttendanceRecord> findBySession_IdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID sessionId);

  List<AttendanceRecord> findByOrganizationMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

  Optional<AttendanceRecord> findByIdAndDeletedAtIsNull(UUID id);

  Optional<AttendanceRecord> findBySession_IdAndOrganizationMember_IdAndDeletedAtIsNull(UUID sessionId, UUID memberId);

  Optional<AttendanceRecord> findBySession_IdAndAthlete_IdAndDeletedAtIsNull(UUID sessionId, UUID athleteId);

  boolean existsBySession_IdAndDeletedAtIsNull(UUID sessionId);
}
