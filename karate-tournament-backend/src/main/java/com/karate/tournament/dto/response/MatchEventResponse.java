package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.ScoreEventType;
import com.karate.tournament.entity.enums.Side;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MatchEventResponse(
    UUID id,
    ScoreEventType type,
    Side side,
    Integer points,
    String penaltyCode,
    Integer judgeNumber,
    Side voteSide,
    String payloadJson,
    Instant occurredAt
) {
}
