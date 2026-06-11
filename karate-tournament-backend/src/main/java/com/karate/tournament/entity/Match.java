package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;

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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "matches")
public class Match extends BaseEntity {
  public static Match create() {
    return new Match();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_id")
  public Tournament tournament;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "category_id")
  public Category category;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bracket_id")
  public Bracket bracket;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tatami_id")
  public Tatami tatami;

  @Column(name = "match_number", nullable = false)
  public Integer matchNumber;

  @Column(name = "round_name", nullable = false, length = 120)
  public String roundName;

  @Column(name = "round_number", nullable = false)
  public Integer roundNumber;

  @Column(name = "bracket_position", nullable = false)
  public Integer bracketPosition = 1;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public MatchStatus status = MatchStatus.SCHEDULED;

  @Column(name = "scheduled_at")
  public Instant scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public CategoryDiscipline mode = CategoryDiscipline.KUMITE;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "winner_entry_id")
  public Entry winnerEntry;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "winner_athlete_id")
  public Athlete winnerAthlete;

  @Enumerated(EnumType.STRING)
  @Column(name = "win_type", length = 60)
  public WinType winType;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "winner_next_match_id")
  public Match winnerNextMatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "winner_next_side", length = 10)
  public Side winnerNextSide;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "loser_next_match_id")
  public Match loserNextMatch;

  @Enumerated(EnumType.STRING)
  @Column(name = "loser_next_side", length = 10)
  public Side loserNextSide;
}
