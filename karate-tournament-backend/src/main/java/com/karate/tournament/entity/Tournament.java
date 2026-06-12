package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.RulesetVersion;
import com.karate.tournament.entity.enums.RulesetPreset;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.entity.enums.TournamentVisibility;

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
import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tournaments")
public class Tournament extends BaseEntity {
  public static Tournament create() {
    return new Tournament();
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "owner_organization_id")
  public Organization ownerOrganization;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "created_by_user_id")
  public AppUser createdByUser;

  @Column(nullable = false, length = 220)
  public String name;

  @Column(length = 80)
  public String code;

  @Column(columnDefinition = "text")
  public String description;

  @Column(length = 255)
  public String location;

  @Column(name = "starts_on")
  public LocalDate startsOn;

  @Column(name = "ends_on")
  public LocalDate endsOn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public TournamentVisibility visibility = TournamentVisibility.PRIVATE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public TournamentStatus status = TournamentStatus.DRAFT;

  @Enumerated(EnumType.STRING)
  @Column(name = "ruleset_version", nullable = false, length = 40)
  public RulesetVersion rulesetVersion = RulesetVersion.WKF_2026;

  @Column(name = "organizer_name", length = 180)
  public String organizerName;

  @Column(name = "tatami_count", nullable = false)
  public Integer tatamiCount = 1;

  @Column(name = "competition_levels", nullable = false, length = 120)
  public String competitionLevels = "PHONG_TRAO,NANG_CAO";

  @Enumerated(EnumType.STRING)
  @Column(name = "ruleset_preset", nullable = false, length = 40)
  public RulesetPreset rulesetPreset = RulesetPreset.WKF;

  @Column(name = "rule_snapshot_json", columnDefinition = "text")
  public String ruleSnapshotJson;

  @Column(name = "registration_deadline")
  public Instant registrationDeadline;

  @Column(name = "registration_fee", precision = 12, scale = 2)
  public BigDecimal registrationFee = BigDecimal.ZERO;

  @Column(nullable = false)
  public short step = 0;

  @Column(name = "phong_trao_enabled", nullable = false)
  public boolean phongTraoEnabled = true;

  @Column(name = "nang_cao_enabled", nullable = false)
  public boolean nangCaoEnabled = true;
}
