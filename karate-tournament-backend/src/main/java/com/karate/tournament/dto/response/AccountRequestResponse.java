package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.AccountRequestStatus;
import com.karate.tournament.entity.enums.PersonGender;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountRequestResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    String organizationCode,
    String displayName,
    String email,
    String phone,
    PersonGender gender,
    LocalDate birthDate,
    String currentAddress,
    AccountRequestStatus status,
    String decisionNote,
    UUID decidedByUserId,
    UUID approvedUserId,
    Instant decidedAt,
    Instant createdAt
) {
}
