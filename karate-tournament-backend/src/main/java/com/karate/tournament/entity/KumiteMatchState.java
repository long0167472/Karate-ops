package com.karate.tournament.entity;

import com.karate.tournament.entity.enums.KumitePenaltyLevel;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.MedicalOutcome;
import com.karate.tournament.entity.enums.MedicalStatus;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.VideoReviewResolution;
import com.karate.tournament.entity.enums.VideoReviewStatus;
import com.karate.tournament.entity.enums.WinType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "kumite_match_state")
public class KumiteMatchState {
  public static KumiteMatchState create() {
    return new KumiteMatchState();
  }

  @Id
  @Column(name = "match_id")
  public UUID matchId;

  @OneToOne(optional = false)
  @MapsId
  @JoinColumn(name = "match_id")
  public Match match;

  @Column(name = "aka_score", nullable = false)
  public int akaScore;

  @Column(name = "ao_score", nullable = false)
  public int aoScore;

  @Column(name = "aka_senshu", nullable = false)
  public boolean akaSenshu;

  @Column(name = "ao_senshu", nullable = false)
  public boolean aoSenshu;

  @Column(name = "aka_chui", nullable = false)
  public int akaChui;

  @Column(name = "ao_chui", nullable = false)
  public int aoChui;

  @Column(name = "aka_hansoku_chui", nullable = false)
  public boolean akaHansokuChui;

  @Column(name = "ao_hansoku_chui", nullable = false)
  public boolean aoHansokuChui;

  @Column(name = "aka_hansoku", nullable = false)
  public boolean akaHansoku;

  @Column(name = "ao_hansoku", nullable = false)
  public boolean aoHansoku;

  @Column(name = "aka_shikkaku", nullable = false)
  public boolean akaShikkaku;

  @Column(name = "ao_shikkaku", nullable = false)
  public boolean aoShikkaku;

  @Column(name = "aka_kiken", nullable = false)
  public boolean akaKiken;

  @Column(name = "ao_kiken", nullable = false)
  public boolean aoKiken;

  @Enumerated(EnumType.STRING)
  @Column(name = "aka_penalty_level", nullable = false, length = 40)
  public KumitePenaltyLevel akaPenaltyLevel = KumitePenaltyLevel.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "ao_penalty_level", nullable = false, length = 40)
  public KumitePenaltyLevel aoPenaltyLevel = KumitePenaltyLevel.NONE;

  @Column(name = "aka_penalty_reason_code", length = 80)
  public String akaPenaltyReasonCode;

  @Column(name = "ao_penalty_reason_code", length = 80)
  public String aoPenaltyReasonCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "aka_category1_penalty", nullable = false, length = 40)
  public KumitePenaltyLevel akaCategory1Penalty = KumitePenaltyLevel.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "ao_category1_penalty", nullable = false, length = 40)
  public KumitePenaltyLevel aoCategory1Penalty = KumitePenaltyLevel.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "aka_category2_penalty", nullable = false, length = 40)
  public KumitePenaltyLevel akaCategory2Penalty = KumitePenaltyLevel.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "ao_category2_penalty", nullable = false, length = 40)
  public KumitePenaltyLevel aoCategory2Penalty = KumitePenaltyLevel.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "senshu_holder", length = 10)
  public Side senshuHolder;

  @Column(name = "senshu_awarded_at")
  public Instant senshuAwardedAt;

  @Column(name = "senshu_revoked", nullable = false)
  public boolean senshuRevoked;

  @Column(name = "senshu_revoked_at")
  public Instant senshuRevokedAt;

  @Column(name = "senshu_revocation_reason_code", length = 80)
  public String senshuRevocationReasonCode;

  @Column(name = "senshu_reaward_blocked", nullable = false)
  public boolean senshuReawardBlocked;

  @Enumerated(EnumType.STRING)
  @Column(name = "video_review_status", nullable = false, length = 40)
  public VideoReviewStatus videoReviewStatus = VideoReviewStatus.IDLE;

  @Enumerated(EnumType.STRING)
  @Column(name = "video_review_active_side", length = 10)
  public Side videoReviewActiveSide;

  @Column(name = "aka_video_review_card_available", nullable = false)
  public boolean akaVideoReviewCardAvailable = true;

  @Column(name = "ao_video_review_card_available", nullable = false)
  public boolean aoVideoReviewCardAvailable = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "video_review_last_resolution", length = 40)
  public VideoReviewResolution videoReviewLastResolution;

  @Enumerated(EnumType.STRING)
  @Column(name = "medical_status", nullable = false, length = 40)
  public MedicalStatus medicalStatus = MedicalStatus.IDLE;

  @Enumerated(EnumType.STRING)
  @Column(name = "medical_injured_side", length = 10)
  public Side medicalInjuredSide;

  @Column(name = "medical_started_at")
  public Instant medicalStartedAt;

  @Column(name = "medical_deadline_at")
  public Instant medicalDeadlineAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "medical_last_outcome", length = 40)
  public MedicalOutcome medicalLastOutcome;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision_winner_side", length = 10)
  public Side decisionWinnerSide;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision_win_type", length = 60)
  public WinType decisionWinType;

  @Column(name = "decision_reason_code", length = 80)
  public String decisionReasonCode;

  @Column(name = "decision_reason_text", length = 255)
  public String decisionReasonText;

  @Column(name = "decision_frozen", nullable = false)
  public boolean decisionFrozen;

  @Column(name = "decision_confirmable", nullable = false)
  public boolean decisionConfirmable;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_live_status", length = 40)
  public MatchStatus lastLiveStatus;

  @Column(name = "duration_ms", nullable = false)
  public int durationMs = 180000;

  @Column(name = "remaining_ms", nullable = false)
  public int remainingMs = 180000;

  @Column(name = "timer_running", nullable = false)
  public boolean timerRunning;

  @Column(name = "timer_started_at")
  public Instant timerStartedAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
