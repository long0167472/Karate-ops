package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.KumitePenaltyLevel;
import com.karate.tournament.entity.enums.MedicalOutcome;
import com.karate.tournament.entity.enums.MedicalStatus;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.VideoReviewResolution;
import com.karate.tournament.entity.enums.VideoReviewStatus;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record KumiteStateResponse(
    int akaScore,
    int aoScore,
    boolean akaSenshu,
    boolean aoSenshu,
    int akaChui,
    int aoChui,
    boolean akaHansokuChui,
    boolean aoHansokuChui,
    boolean akaHansoku,
    boolean aoHansoku,
    boolean akaShikkaku,
    boolean aoShikkaku,
    boolean akaKiken,
    boolean aoKiken,
    int durationMs,
    int remainingMs,
    boolean timerRunning,
    Instant timerStartedAt,
    KumiteDecisionResponse decision,
    KumiteSenshuResponse senshu,
    KumitePenaltyStateResponse penalties,
    VideoReviewStateResponse videoReview,
    MedicalStateResponse medical
) {
  public record KumiteDecisionResponse(
      Side winnerSide,
      WinType winType,
      String reasonCode,
      String reasonText,
      boolean frozen,
      boolean confirmable
  ) {
  }

  public record KumiteSenshuResponse(
      Side holderSide,
      Instant awardedAt,
      boolean revoked,
      Instant revokedAt,
      String revocationReasonCode
  ) {
  }

  public record KumitePenaltyStateResponse(
      SidePenaltyResponse aka,
      SidePenaltyResponse ao
  ) {
  }

  public record SidePenaltyResponse(
      KumitePenaltyLevel penaltyLevel,
      String reasonCode,
      KumitePenaltyLevel category1Level,
      KumitePenaltyLevel category2Level,
      boolean hansoku,
      boolean shikkaku,
      boolean kiken
  ) {
  }

  public record VideoReviewStateResponse(
      Side activeRequestSide,
      VideoReviewStatus status,
      boolean akaCardAvailable,
      boolean aoCardAvailable,
      VideoReviewResolution lastResolution
  ) {
  }

  public record MedicalStateResponse(
      Side injuredSide,
      Instant startedAt,
      Instant deadlineAt,
      MedicalStatus status,
      MedicalOutcome lastOutcome
  ) {
  }
}
