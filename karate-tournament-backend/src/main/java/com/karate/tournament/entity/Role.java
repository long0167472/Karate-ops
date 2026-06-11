package com.karate.tournament.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {
  public static Role create() {
    return new Role();
  }

  @Column(nullable = false, unique = true, length = 60)
  public String code;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(length = 255)
  public String description;
}
