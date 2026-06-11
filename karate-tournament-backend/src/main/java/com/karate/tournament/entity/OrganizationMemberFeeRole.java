package com.karate.tournament.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "organization_member_fee_roles")
public class OrganizationMemberFeeRole extends BaseEntity {
  public static OrganizationMemberFeeRole create() {
    return new OrganizationMemberFeeRole();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_member_id")
  public OrganizationMember member;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "fee_role_id")
  public ClubFeeRole feeRole;
}
