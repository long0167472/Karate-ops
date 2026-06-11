package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.ExpenseDisbursementStatus;
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
@Table(name = "club_finance_expenses")
public class ClubFinanceExpense extends BaseEntity {
  public static ClubFinanceExpense create() {
    return new ClubFinanceExpense();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(nullable = false, length = 180)
  public String name;

  @Column(nullable = false)
  public BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "expense_date")
  public LocalDate expenseDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 60)
  public ExpenseDisbursementStatus status = ExpenseDisbursementStatus.PENDING_DISBURSEMENT;

  @Column(length = 500)
  public String note;
}
