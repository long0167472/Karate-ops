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
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "belt_exam_scores")
public class BeltExamScore extends BaseEntity {
  public static BeltExamScore create() {
    return new BeltExamScore();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "candidate_id")
  public BeltExamCandidate candidate;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "criterion_id")
  public BeltExamCriterion criterion;

  @Column(nullable = false, precision = 6, scale = 2)
  public BigDecimal score = BigDecimal.ZERO;

  @Column(length = 300)
  public String note;
}
