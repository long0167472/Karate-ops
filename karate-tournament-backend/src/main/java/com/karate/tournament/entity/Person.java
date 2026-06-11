package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.PersonGender;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "persons")
public class Person extends BaseEntity {
  public static Person create() {
    return new Person();
  }

  @Column(name = "display_name", nullable = false, length = 180)
  public String displayName;

  @Column(name = "first_name", length = 90)
  public String firstName;

  @Column(name = "last_name", length = 90)
  public String lastName;

  @Column(name = "birth_date")
  public LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  public PersonGender gender;

  @Column(name = "national_id", length = 80)
  public String nationalId;

  @Column(length = 180)
  public String email;

  @Column(length = 60)
  public String phone;

  @Column(name = "current_address", length = 255)
  public String currentAddress;

  @Column(name = "emergency_contact_name", length = 180)
  public String emergencyContactName;

  @Column(name = "emergency_contact_phone", length = 60)
  public String emergencyContactPhone;
}
