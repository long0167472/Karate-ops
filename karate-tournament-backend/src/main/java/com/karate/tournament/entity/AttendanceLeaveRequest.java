package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.LeaveRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "attendance_leave_requests")
public class AttendanceLeaveRequest extends BaseEntity {
  public static AttendanceLeaveRequest create() {
    return new AttendanceLeaveRequest();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "attendance_session_id")
  public AttendanceSession session;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_member_id")
  public OrganizationMember member;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "requester_user_id")
  public AppUser requesterUser;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "decided_by_user_id")
  public AppUser decidedByUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public LeaveRequestStatus status = LeaveRequestStatus.PENDING;

  @Column(nullable = false, length = 500)
  public String reason;

  @Column(name = "decision_note", length = 500)
  public String decisionNote;

  @Column(name = "decided_at")
  public Instant decidedAt;
}
