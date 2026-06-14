package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.BeltExamStatus;

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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "belt_exams")
public class BeltExam extends BaseEntity {
  public static BeltExam create() {
    return new BeltExam();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(nullable = false, length = 180)
  public String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public BeltExamStatus status = BeltExamStatus.DRAFT;

  @Column(name = "exam_date")
  public LocalDate examDate;

  @Column(length = 180)
  public String location;

  @Column(name = "examiner_name", length = 180)
  public String examinerName;

  @Column(name = "pass_threshold", precision = 6, scale = 2)
  public BigDecimal passThreshold;

  @Column(length = 500)
  public String notes;
}
