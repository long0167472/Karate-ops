package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.AccountRequestStatus;
import com.karate.tournament.entity.enums.PersonGender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "account_requests")
public class AccountRequest extends BaseEntity {
  public static AccountRequest create() {
    return new AccountRequest();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @Column(name = "display_name", nullable = false, length = 180)
  public String displayName;

  @Column(nullable = false, length = 180)
  public String email;

  @Column(nullable = false, length = 60)
  public String phone;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  public PersonGender gender;

  @Column(name = "birth_date")
  public LocalDate birthDate;

  @Column(name = "current_address", length = 255)
  public String currentAddress;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public AccountRequestStatus status = AccountRequestStatus.PENDING;

  @Column(name = "decision_note", length = 500)
  public String decisionNote;

  @Column(name = "decided_at")
  public Instant decidedAt;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "decided_by_user_id")
  public AppUser decidedByUser;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "approved_user_id")
  public AppUser approvedUser;
}
