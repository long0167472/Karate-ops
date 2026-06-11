package com.karate.tournament.repository;

import com.karate.tournament.entity.AttendanceLeaveRequest;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceLeaveRequestRepository extends JpaRepository<AttendanceLeaveRequest, UUID> {
  Optional<AttendanceLeaveRequest> findByIdAndDeletedAtIsNull(UUID id);

  Optional<AttendanceLeaveRequest> findBySession_IdAndMember_IdAndDeletedAtIsNull(UUID sessionId, UUID memberId);

  List<AttendanceLeaveRequest> findByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

  List<AttendanceLeaveRequest> findBySession_Organization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<AttendanceLeaveRequest> findBySession_Organization_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID organizationId,
      LeaveRequestStatus status
  );
}
