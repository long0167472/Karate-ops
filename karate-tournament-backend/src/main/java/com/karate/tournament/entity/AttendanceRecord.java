package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.AttendanceRecordStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord extends BaseEntity {
  public static AttendanceRecord create() {
    return new AttendanceRecord();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "attendance_session_id")
  public AttendanceSession session;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "organization_member_id")
  public OrganizationMember organizationMember;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "athlete_id")
  public Athlete athlete;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public AttendanceRecordStatus status = AttendanceRecordStatus.PRESENT;

  @Column(name = "check_in_at")
  public Instant checkInAt;

  @Column(length = 500)
  public String note;
}
