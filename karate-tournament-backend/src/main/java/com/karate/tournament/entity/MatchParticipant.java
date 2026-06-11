package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.Side;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "match_participants")
public class MatchParticipant extends BaseEntity {
  public static MatchParticipant create() {
    return new MatchParticipant();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "match_id")
  public Match match;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "entry_id")
  public Entry entry;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  public Side side;
}
