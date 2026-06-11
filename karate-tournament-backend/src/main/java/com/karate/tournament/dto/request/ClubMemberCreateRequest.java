package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.PaymentStatus;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record ClubMemberCreateRequest(
    UUID personId,
    UUID userId,
    ClubMemberRole role,
    ClubMemberStatus status,
    LocalDate joinedAt,
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
