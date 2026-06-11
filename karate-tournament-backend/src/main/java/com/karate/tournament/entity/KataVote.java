package com.karate.tournament.entity;


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
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "kata_votes")
public class KataVote extends BaseEntity {
  public static KataVote create() {
    return new KataVote();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "match_id")
  public Match match;

  @Column(name = "judge_number", nullable = false)
  public Integer judgeNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  public Side side;

  @Column(name = "vote_value", precision = 5, scale = 2)
  public BigDecimal voteValue;
}
