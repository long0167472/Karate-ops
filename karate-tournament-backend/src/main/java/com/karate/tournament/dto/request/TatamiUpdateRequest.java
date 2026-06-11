package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.TatamiStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record TatamiUpdateRequest(
    Integer tatamiNo,
    String name,
    TatamiStatus status
) {
}
