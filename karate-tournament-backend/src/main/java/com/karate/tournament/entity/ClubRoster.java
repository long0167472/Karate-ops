package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.ClubRosterStatus;

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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_roster")
public class ClubRoster extends BaseEntity {
  public static ClubRoster create() {
    return new ClubRoster();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "athlete_id")
  public Athlete athlete;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public ClubRosterStatus status = ClubRosterStatus.ACTIVE;

  @Column(name = "joined_at")
  public LocalDate joinedAt;
}
