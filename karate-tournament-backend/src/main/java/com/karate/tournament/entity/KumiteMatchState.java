package com.karate.tournament.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "kumite_match_state")
public class KumiteMatchState {
  public static KumiteMatchState create() {
    return new KumiteMatchState();
  }

  @Id
  @Column(name = "match_id")
  public UUID matchId;

  @OneToOne(optional = false)
  @MapsId
  @JoinColumn(name = "match_id")
  public Match match;

  @Column(name = "aka_score", nullable = false)
  public int akaScore;

  @Column(name = "ao_score", nullable = false)
  public int aoScore;

  @Column(name = "aka_senshu", nullable = false)
  public boolean akaSenshu;

  @Column(name = "ao_senshu", nullable = false)
  public boolean aoSenshu;

  @Column(name = "aka_chui", nullable = false)
  public int akaChui;

  @Column(name = "ao_chui", nullable = false)
  public int aoChui;

  @Column(name = "aka_hansoku_chui", nullable = false)
  public boolean akaHansokuChui;

  @Column(name = "ao_hansoku_chui", nullable = false)
  public boolean aoHansokuChui;

  @Column(name = "aka_hansoku", nullable = false)
  public boolean akaHansoku;

  @Column(name = "ao_hansoku", nullable = false)
  public boolean aoHansoku;

  @Column(name = "aka_shikkaku", nullable = false)
  public boolean akaShikkaku;

  @Column(name = "ao_shikkaku", nullable = false)
  public boolean aoShikkaku;

  @Column(name = "aka_kiken", nullable = false)
  public boolean akaKiken;

  @Column(name = "ao_kiken", nullable = false)
  public boolean aoKiken;

  @Column(name = "duration_ms", nullable = false)
  public int durationMs = 180000;

  @Column(name = "remaining_ms", nullable = false)
  public int remainingMs = 180000;

  @Column(name = "timer_running", nullable = false)
  public boolean timerRunning;

  @Column(name = "timer_started_at")
  public Instant timerStartedAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
