package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.PaymentStatus;
import com.karate.tournament.entity.enums.PersonGender;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubMemberResponse(
    UUID id,
    UUID organizationId,
    String organizationName,
    UUID personId,
    String personName,
    UUID userId,
    String userName,
    ClubMemberRole role,
    ClubMemberStatus status,
    LocalDate joinedAt,
    PersonGender gender,
    String phone,
    String email,
    String currentAddress,
    Boolean student,
    Boolean attendanceViewEnabled,
    PaymentStatus tuitionStatus,
    BigDecimal tuitionPaidAmount,
    PaymentStatus otherFeeStatus,
    BigDecimal otherFeePaidAmount,
    String paymentNote,
    String memberNote
) {
}
