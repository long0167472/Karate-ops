package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.PaymentStatus;
import com.karate.tournament.entity.enums.PersonGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberAccountCreateRequest(
    @NotBlank String displayName,
    @NotBlank @Email String email,
    @NotBlank String phone,
    PersonGender gender,
    LocalDate birthDate,
    String currentAddress,
    String emergencyContactName,
    String emergencyContactPhone,
    ClubMemberRole role,
    ClubMemberStatus status,
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
