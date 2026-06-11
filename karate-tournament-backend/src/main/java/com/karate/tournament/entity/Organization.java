package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.OrganizationStatus;
import com.karate.tournament.entity.enums.OrganizationType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {
  public static Organization create() {
    return new Organization();
  }

  @Column(nullable = false, length = 180)
  public String name;

  @Column(name = "short_name", length = 80)
  public String shortName;

  @Column(length = 60)
  public String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public OrganizationType type = OrganizationType.CLUB;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public OrganizationStatus status = OrganizationStatus.ACTIVE;

  @Column(length = 80)
  public String country;

  @Column(length = 120)
  public String province;

  @Column(length = 255)
  public String address;

  @Column(name = "contact_email", length = 160)
  public String contactEmail;

  @Column(name = "contact_phone", length = 60)
  public String contactPhone;
}
