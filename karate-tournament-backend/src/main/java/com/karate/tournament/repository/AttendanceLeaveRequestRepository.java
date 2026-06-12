package com.karate.tournament.repository;

import com.karate.tournament.entity.AttendanceLeaveRequest;
import com.karate.tournament.entity.enums.LeaveRequestStatus;
import com.karate.tournament.entity.enums.LeaveRequestType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceLeaveRequestRepository extends JpaRepository<AttendanceLeaveRequest, UUID> {
  Optional<AttendanceLeaveRequest> findByIdAndDeletedAtIsNull(UUID id);

  /**
   * Locks the request row for the duration of the surrounding transaction so concurrent
   * decisions (approve/reject/expire) serialize instead of double-applying side effects.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from AttendanceLeaveRequest r where r.id = :id and r.deletedAt is null")
  Optional<AttendanceLeaveRequest> findWithLockById(@Param("id") UUID id);

  Optional<AttendanceLeaveRequest> findBySession_IdAndMember_IdAndDeletedAtIsNull(UUID sessionId, UUID memberId);

  /**
   * Locks the existing (session, member) request during re-submission so a member cannot
   * race a manager decision while rewriting the same request.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from AttendanceLeaveRequest r where r.session.id = :sessionId and r.member.id = :memberId and r.deletedAt is null")
  Optional<AttendanceLeaveRequest> findWithLockBySessionAndMember(@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);

  List<AttendanceLeaveRequest> findByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

  List<AttendanceLeaveRequest> findByMember_IdInAndDeletedAtIsNullOrderByCreatedAtDesc(List<UUID> memberIds);

  List<AttendanceLeaveRequest> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<AttendanceLeaveRequest> findByOrganization_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID organizationId,
      LeaveRequestStatus status
  );

  List<AttendanceLeaveRequest> findByMember_IdAndRequestTypeAndStatusAndDeletedAtIsNull(
      UUID memberId,
      LeaveRequestType requestType,
      LeaveRequestStatus status
  );

  List<AttendanceLeaveRequest> findByStatusAndExpiresAtBeforeAndDeletedAtIsNull(LeaveRequestStatus status, Instant cutoff);
}
