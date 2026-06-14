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
@Table(name = "belt_exam_criteria")
public class BeltExamCriterion extends BaseEntity {
  public static BeltExamCriterion create() {
    return new BeltExamCriterion();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "belt_exam_id")
  public BeltExam exam;

  @Column(nullable = false, length = 180)
  public String name;

  @Column(length = 300)
  public String description;

  @Column(name = "max_score", nullable = false, precision = 6, scale = 2)
  public BigDecimal maxScore = new BigDecimal("10.00");

  @Column(name = "weight", nullable = false, precision = 6, scale = 2)
  public BigDecimal weight = BigDecimal.ONE;

  @Column(name = "display_order", nullable = false)
  public int displayOrder = 0;
}
