package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.AthleteStatus;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "athletes")
public class Athlete extends BaseEntity {
  public static Athlete create() {
    return new Athlete();
  }

  @OneToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "person_id")
  public Person person;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "primary_organization_id")
  public Organization primaryOrganization;

  @Column(name = "external_code", length = 80)
  public String externalCode;

  @Column(length = 80)
  public String belt;

  @Column(name = "weight_kg", precision = 6, scale = 2)
  public BigDecimal weightKg;

  @Column(name = "height_cm", precision = 6, scale = 2)
  public BigDecimal heightCm;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public AthleteStatus status = AthleteStatus.ACTIVE;
}
