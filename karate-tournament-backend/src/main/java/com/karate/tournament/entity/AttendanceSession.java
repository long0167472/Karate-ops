package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.AttendanceSessionSource;
import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;

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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession extends BaseEntity {
  public static AttendanceSession create() {
    return new AttendanceSession();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tournament_participant_id")
  public TournamentParticipant tournamentParticipant;

  @Column(nullable = false, length = 180)
  public String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "session_type", nullable = false, length = 40)
  public AttendanceSessionType type = AttendanceSessionType.TRAINING;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public AttendanceSessionStatus status = AttendanceSessionStatus.OPEN;

  @Column(name = "scheduled_at")
  public Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public AttendanceSessionSource source = AttendanceSessionSource.MANUAL;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "training_schedule_id")
  public ClubTrainingSchedule trainingSchedule;

  @Column(name = "scheduled_date")
  public LocalDate scheduledDate;

  @Column(length = 500)
  public String notes;
}
