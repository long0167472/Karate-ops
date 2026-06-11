package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.CompetitionLevel;
import com.karate.tournament.entity.enums.EntryType;
import com.karate.tournament.entity.enums.PersonGender;
import com.karate.tournament.entity.enums.RulesetVersion;

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
@Table(name = "categories")
public class Category extends BaseEntity {
  public static Category create() {
    return new Category();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_id")
  public Tournament tournament;

  @Column(nullable = false, length = 180)
  public String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public CategoryDiscipline discipline = CategoryDiscipline.KUMITE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public PersonGender gender = PersonGender.OPEN;

  @Column(name = "age_min")
  public Integer ageMin;

  @Column(name = "age_max")
  public Integer ageMax;

  @Column(name = "weight_min_kg", precision = 6, scale = 2)
  public BigDecimal weightMinKg;

  @Column(name = "weight_max_kg", precision = 6, scale = 2)
  public BigDecimal weightMaxKg;

  @Enumerated(EnumType.STRING)
  @Column(name = "competition_level", nullable = false, length = 40)
  public CompetitionLevel competitionLevel = CompetitionLevel.OPEN;

  @Column(name = "weight_label", length = 40)
  public String weightLabel;

  @Column(name = "open_weight", nullable = false)
  public Boolean openWeight = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, length = 40)
  public EntryType entryType = EntryType.INDIVIDUAL;

  @Column(nullable = false, length = 40)
  public String status = "DRAFT";

  @Enumerated(EnumType.STRING)
  @Column(name = "ruleset_version", nullable = false, length = 40)
  public RulesetVersion rulesetVersion = RulesetVersion.WKF_2026;

  @Column(name = "repechage_enabled", nullable = false)
  public Boolean repechageEnabled = true;

  @Column(name = "match_duration_seconds", nullable = false)
  public Integer matchDurationSeconds = 180;

  @Column(name = "kata_judge_count", nullable = false)
  public Integer kataJudgeCount = 5;

  @Column(name = "kata_repeat_allowed", nullable = false)
  public Boolean kataRepeatAllowed = false;

  @Column(name = "entry_limit_per_organization")
  public Integer entryLimitPerOrganization;
}
