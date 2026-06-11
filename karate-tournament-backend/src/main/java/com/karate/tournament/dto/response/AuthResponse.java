package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    AuthUserResponse user
) {
}
