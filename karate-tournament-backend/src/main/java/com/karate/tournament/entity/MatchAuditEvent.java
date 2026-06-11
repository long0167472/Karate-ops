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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "match_audit_events")
public class MatchAuditEvent extends BaseEntity {
  public static MatchAuditEvent create() {
    return new MatchAuditEvent();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "match_id")
  public Match match;

  @Column(name = "actor_user_id")
  public UUID actorUserId;

  @Column(nullable = false, length = 80)
  public String action;

  @Column(length = 255)
  public String reason;

  @Column(name = "payload_json", columnDefinition = "text")
  public String payloadJson;
}
