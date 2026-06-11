package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.PersonGender;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record PersonUpdateRequest(
    String displayName,
    String firstName,
    String lastName,
    LocalDate birthDate,
    PersonGender gender,
    String nationalId,
    String email,
    String phone,
    String currentAddress,
    String emergencyContactName,
    String emergencyContactPhone
) {
}
