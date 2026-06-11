package com.karate.tournament.entity;


import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
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
@Table(name = "organization_members")
public class OrganizationMember extends BaseEntity {
  public static OrganizationMember create() {
    return new OrganizationMember();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  public AppUser user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "person_id")
  public Person person;

  @Enumerated(EnumType.STRING)
  @Column(name = "role_code", nullable = false, length = 60)
  public ClubMemberRole role = ClubMemberRole.ATHLETE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public ClubMemberStatus status = ClubMemberStatus.ACTIVE;

  @Column(name = "joined_at")
  public LocalDate joinedAt;

  @Column(name = "is_student", nullable = false)
  public boolean student = false;

  @Column(name = "attendance_view_enabled", nullable = false)
  public boolean attendanceViewEnabled = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "tuition_status", nullable = false, length = 40)
  public PaymentStatus tuitionStatus = PaymentStatus.PENDING;

  @Column(name = "tuition_paid_amount", nullable = false)
  public BigDecimal tuitionPaidAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "other_fee_status", nullable = false, length = 40)
  public PaymentStatus otherFeeStatus = PaymentStatus.PENDING;

  @Column(name = "other_fee_paid_amount", nullable = false)
  public BigDecimal otherFeePaidAmount = BigDecimal.ZERO;

  @Column(name = "payment_note", length = 500)
  public String paymentNote;

  @Column(name = "member_note", length = 500)
  public String memberNote;
}
