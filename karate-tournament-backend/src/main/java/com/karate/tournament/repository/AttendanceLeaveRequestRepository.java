package com.karate.tournament.repository;

import com.karate.tournament.entity.AttendanceLeaveRequest;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
import com.karate.tournament.entity.enums.LeaveRequestType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceLeaveRequestRepository extends JpaRepository<AttendanceLeaveRequest, UUID> {
  Optional<AttendanceLeaveRequest> findByIdAndDeletedAtIsNull(UUID id);

  Optional<AttendanceLeaveRequest> findBySession_IdAndMember_IdAndDeletedAtIsNull(UUID sessionId, UUID memberId);

  List<AttendanceLeaveRequest> findByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

  List<AttendanceLeaveRequest> findByMember_Organization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<AttendanceLeaveRequest> findByMember_Organization_IdAndRequestTypeAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID organizationId,
      LeaveRequestType requestType,
      LeaveRequestStatus status
  );

  List<AttendanceLeaveRequest> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(LeaveRequestStatus status);
}
