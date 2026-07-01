package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ConfirmResultRequest(
    Side winnerSide,
    WinType winType,
    String reason
) {
}
