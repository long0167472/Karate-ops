package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.ScoreEventType;
import com.karate.tournament.entity.enums.Side;

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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "match_score_events")
public class MatchScoreEvent extends BaseEntity {
  public static MatchScoreEvent create() {
    return new MatchScoreEvent();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "match_id")
  public Match match;

  @Column(name = "actor_user_id")
  public UUID actorUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  public ScoreEventType type;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  public Side side;

  public Integer points;

  @Column(name = "penalty_code", length = 60)
  public String penaltyCode;

  @Column(name = "judge_number")
  public Integer judgeNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "vote_side", length = 10)
  public Side voteSide;

  @Column(name = "payload_json", columnDefinition = "text")
  public String payloadJson;

  @Column(name = "occurred_at", nullable = false)
  public Instant occurredAt = Instant.now();
}
