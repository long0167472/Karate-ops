package com.karate.tournament.dto.request;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record RegisterClubManagerRequest(
    @NotBlank String displayName,
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String clubName,
    String phone
) {
}
