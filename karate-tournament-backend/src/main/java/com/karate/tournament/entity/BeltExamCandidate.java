package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.BeltExamResult;

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
@Table(name = "belt_exam_candidates")
public class BeltExamCandidate extends BaseEntity {
  public static BeltExamCandidate create() {
    return new BeltExamCandidate();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "belt_exam_id")
  public BeltExam exam;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "organization_member_id")
  public OrganizationMember organizationMember;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "athlete_id")
  public Athlete athlete;

  @Column(name = "current_belt", length = 80)
  public String currentBelt;

  @Column(name = "target_belt", nullable = false, length = 80)
  public String targetBelt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public BeltExamResult result = BeltExamResult.PENDING;

  @Column(name = "examiner_note", length = 500)
  public String examinerNote;

  @Column(name = "belt_applied", nullable = false)
  public boolean beltApplied = false;
}
