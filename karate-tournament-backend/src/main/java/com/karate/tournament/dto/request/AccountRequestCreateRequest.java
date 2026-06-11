package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.PersonGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AccountRequestCreateRequest(
    @NotBlank String organizationCode,
    @NotBlank String displayName,
    @NotBlank @Email String email,
    @NotBlank String phone,
    PersonGender gender,
    LocalDate birthDate,
    String currentAddress
) {
}
