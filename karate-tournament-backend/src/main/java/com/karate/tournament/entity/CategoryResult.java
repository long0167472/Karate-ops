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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "category_results")
public class CategoryResult extends BaseEntity {
  public static CategoryResult create() {
    return new CategoryResult();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "category_id")
  public Category category;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "entry_id")
  public Entry entry;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "athlete_id")
  public Athlete athlete;

  @Column(name = "team_id")
  public UUID teamId;

  @Column(nullable = false)
  public Integer placement;

  @Column(length = 20)
  public String medal;
}
