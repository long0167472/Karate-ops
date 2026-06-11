package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.FeeAssignmentSource;
import com.karate.tournament.entity.enums.PaymentStatus;

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
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member_fee_assignments")
public class MemberFeeAssignment extends BaseEntity {
  public static MemberFeeAssignment create() {
    return new MemberFeeAssignment();
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

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "assigned_role_id")
  public ClubFeeRole assignedRole;

  @Column(name = "amount_due", nullable = false)
  public BigDecimal amountDue = BigDecimal.ZERO;

  @Column(name = "paid_amount", nullable = false)
  public BigDecimal paidAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public PaymentStatus status = PaymentStatus.PENDING;

  @Column(name = "due_date")
  public LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public FeeAssignmentSource source = FeeAssignmentSource.RULE;

  @Column(length = 500)
  public String note;
}
