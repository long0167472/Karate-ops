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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_fee_roles")
public class ClubFeeRole extends BaseEntity {
  public static ClubFeeRole create() {
    return new ClubFeeRole();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(nullable = false, length = 80)
  public String code;

  @Column(nullable = false, length = 160)
  public String name;

  @Column(length = 500)
  public String description;

  @Column(nullable = false)
  public int priority = 100;

  @Column(nullable = false)
  public boolean active = true;
}
