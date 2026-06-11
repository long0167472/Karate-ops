package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record TatamiDashboardRow(
    UUID tatamiId,
    Integer tatamiNo,
    String name,
    long scheduled,
    long running,
    long completed,
    UUID currentMatchId
) {
}
