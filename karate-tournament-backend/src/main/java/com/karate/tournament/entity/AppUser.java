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
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {
  public static AppUser create() {
    return new AppUser();
  }

  @Column(name = "display_name", nullable = false, length = 180)
  public String displayName;

  @Column(length = 180)
  public String email;

  @Column(length = 120)
  public String username;

  @Column(length = 60)
  public String phone;

  @Column(nullable = false, length = 40)
  public String status = "ACTIVE";

  @Column(name = "password_hash", length = 120)
  public String passwordHash;

  @Column(name = "last_login_at")
  public Instant lastLoginAt;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "primary_organization_id")
  public Organization primaryOrganization;
}
