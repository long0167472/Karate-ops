package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.KumitePenaltyCategory;
import com.karate.tournament.entity.enums.KumitePenaltyLevel;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.MedicalOutcome;
import com.karate.tournament.entity.enums.ScoreEventType;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.VideoReviewResolution;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MatchEventRequest(
    @NotNull ScoreEventType type,
    Side side,
    Integer points,
    String penaltyCode,
    @Min(1) @Max(7) Integer judgeNumber,
    Side voteSide,
    Integer timerMs,
    MatchStatus status,
    KumitePenaltyCategory penaltyCategory,
    KumitePenaltyLevel penaltyLevel,
    String penaltyReasonCode,
    VideoReviewResolution resolution,
    Side resolutionSide,
    Integer resolutionPoints,
    String reasonCode,
    String reasonText,
    MedicalOutcome medicalOutcome,
    String exchangeId,
    String payloadJson
) {
}
