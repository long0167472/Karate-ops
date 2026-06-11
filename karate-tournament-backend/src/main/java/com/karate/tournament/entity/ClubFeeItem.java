package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.BillingCycle;
import com.karate.tournament.entity.enums.FeeItemKind;
import com.karate.tournament.entity.enums.FeeItemStatus;
import com.karate.tournament.entity.enums.FeeItemType;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_fee_items")
public class ClubFeeItem extends BaseEntity {
  public static ClubFeeItem create() {
    return new ClubFeeItem();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(nullable = false, length = 180)
  public String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "fee_type", nullable = false, length = 60)
  public FeeItemType feeType = FeeItemType.TUITION;

  @Enumerated(EnumType.STRING)
  @Column(name = "fee_kind", nullable = false, length = 60)
  public FeeItemKind feeKind = FeeItemKind.ONE_TIME_INCOME;

  @Enumerated(EnumType.STRING)
  @Column(name = "billing_cycle", nullable = false, length = 60)
  public BillingCycle billingCycle = BillingCycle.MONTHLY;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public FeeItemStatus status = FeeItemStatus.ACTIVE;

  @Column(name = "default_amount", nullable = false)
  public BigDecimal defaultAmount = BigDecimal.ZERO;

  @Column(name = "due_day")
  public Integer dueDay;

  @Column(length = 500)
  public String description;
}
