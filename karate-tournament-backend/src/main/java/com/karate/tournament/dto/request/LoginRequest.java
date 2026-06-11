package com.karate.tournament.dto.request;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {
}
