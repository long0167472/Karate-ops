package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.ScoreEventType;
import com.karate.tournament.entity.enums.Side;

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
    String payloadJson
) {
}
