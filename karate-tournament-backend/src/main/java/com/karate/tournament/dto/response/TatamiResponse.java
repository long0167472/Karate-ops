package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.TatamiStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record TatamiResponse(
    UUID id,
    UUID tournamentId,
    Integer tatamiNo,
    String name,
    TatamiStatus status,
    UUID currentMatchId
) {
}
