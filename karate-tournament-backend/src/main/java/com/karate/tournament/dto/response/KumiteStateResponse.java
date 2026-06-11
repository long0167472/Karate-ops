package com.karate.tournament.dto.response;

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
    Instant timerStartedAt
) {
}
