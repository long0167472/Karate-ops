package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.BracketStatus;
import com.karate.tournament.entity.enums.BracketType;

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
@Table(name = "brackets")
public class Bracket extends BaseEntity {
  public static Bracket create() {
    return new Bracket();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "category_id")
  public Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public BracketType type = BracketType.REPECHAGE;

  @Column(nullable = false)
  public Integer size;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public BracketStatus status = BracketStatus.DRAFT;
}
