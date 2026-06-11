package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.TatamiStatus;

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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tatamis")
public class Tatami extends BaseEntity {
  public static Tatami create() {
    return new Tatami();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_id")
  public Tournament tournament;

  @Column(name = "tatami_no", nullable = false)
  public Integer tatamiNo;

  @Column(nullable = false, length = 120)
  public String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public TatamiStatus status = TatamiStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "current_match_id")
  public Match currentMatch;
}
