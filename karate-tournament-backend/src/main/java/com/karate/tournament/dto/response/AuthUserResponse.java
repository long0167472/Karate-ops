package com.karate.tournament.dto.response;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record AuthUserResponse(
    UUID id,
    String displayName,
    String email,
    String username,
    String phone,
    UUID primaryOrganizationId,
    String primaryOrganizationName,
    String status,
    List<String> roles
) {
}
