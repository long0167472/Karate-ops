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
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_fee_item_role_amounts")
public class ClubFeeItemRoleAmount extends BaseEntity {
  public static ClubFeeItemRoleAmount create() {
    return new ClubFeeItemRoleAmount();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "fee_item_id")
  public ClubFeeItem feeItem;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "fee_role_id")
  public ClubFeeRole feeRole;

  @Column(nullable = false)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(nullable = false)
  public boolean exempt = false;
}
