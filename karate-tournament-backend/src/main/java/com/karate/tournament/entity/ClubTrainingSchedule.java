package com.karate.tournament.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_training_schedules")
public class ClubTrainingSchedule extends BaseEntity {
  public static ClubTrainingSchedule create() {
    return new ClubTrainingSchedule();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(nullable = false, length = 180)
  public String name;

  @Column(name = "days_of_week", nullable = false, length = 40)
  public String daysOfWeek = "";

  @Column(name = "start_time", nullable = false)
  public LocalTime startTime = LocalTime.of(18, 30);

  @Column(name = "duration_minutes", nullable = false)
  public int durationMinutes = 90;

  @Column(nullable = false, length = 80)
  public String timezone = "Asia/Ho_Chi_Minh";

  @Column(nullable = false)
  public boolean active = true;
}
