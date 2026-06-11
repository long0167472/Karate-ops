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
@Table(name = "user_role_assignments")
public class UserRoleAssignment extends BaseEntity {
  public static UserRoleAssignment create() {
    return new UserRoleAssignment();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "user_id")
  public AppUser user;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id")
  public Role role;

  @Column(name = "scope_type", nullable = false, length = 40)
  public String scopeType = "GLOBAL";

  @Column(name = "scope_id")
  public UUID scopeId;
}
