package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.EntryStatus;
import com.karate.tournament.entity.enums.WeighInStatus;

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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "entries")
public class Entry extends BaseEntity {
  public static Entry create() {
    return new Entry();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "category_id")
  public Category category;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_participant_id")
  public TournamentParticipant tournamentParticipant;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "athlete_id")
  public Athlete athlete;

  @Column(name = "team_id")
  public UUID teamId;

  @Column(name = "seed_no")
  public Integer seedNo;

  @Column(name = "registration_weight_kg", precision = 6, scale = 2)
  public BigDecimal registrationWeightKg;

  @Enumerated(EnumType.STRING)
  @Column(name = "weigh_in_status", nullable = false, length = 40)
  public WeighInStatus weighInStatus = WeighInStatus.NEEDS_ORGANIZER_REVIEW;

  @Column(name = "team_name", length = 180)
  public String teamName;

  @Column(name = "team_member_athlete_ids", columnDefinition = "text")
  public String teamMemberAthleteIds;

  @Column(name = "validation_notes", columnDefinition = "text")
  public String validationNotes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public EntryStatus status = EntryStatus.REGISTERED;
}
