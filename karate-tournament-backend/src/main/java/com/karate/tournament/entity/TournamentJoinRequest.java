package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.TournamentJoinRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tournament_join_requests")
public class TournamentJoinRequest extends BaseEntity {
  public static TournamentJoinRequest create() {
    return new TournamentJoinRequest();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "tournament_id")
  public Tournament tournament;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_member_id")
  public OrganizationMember member;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "requester_user_id")
  public AppUser requesterUser;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "decided_by_user_id")
  public AppUser decidedByUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  public TournamentJoinRequestStatus status = TournamentJoinRequestStatus.PENDING;

  @Column(length = 500)
  public String note;

  @Column(name = "decision_note", length = 500)
  public String decisionNote;

  @Column(name = "decided_at")
  public Instant decidedAt;
}
