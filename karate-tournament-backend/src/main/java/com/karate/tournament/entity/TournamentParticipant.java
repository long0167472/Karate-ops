package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.ParticipantStatus;

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
@Table(name = "tournament_participants")
public class TournamentParticipant extends BaseEntity {
  public static TournamentParticipant create() {
    return new TournamentParticipant();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_id")
  public Tournament tournament;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(name = "display_name", nullable = false, length = 180)
  public String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public ParticipantStatus status = ParticipantStatus.REQUESTED;

  @Column(name = "approved_at")
  public Instant approvedAt;
}
