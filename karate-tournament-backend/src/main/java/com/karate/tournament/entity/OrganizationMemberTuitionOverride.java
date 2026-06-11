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
@Table(name = "organization_member_tuition_overrides")
public class OrganizationMemberTuitionOverride extends BaseEntity {
  public static OrganizationMemberTuitionOverride create() {
    return new OrganizationMemberTuitionOverride();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_member_id")
  public OrganizationMember member;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "fee_item_id")
  public ClubFeeItem feeItem;
}
