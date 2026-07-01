package com.karate.tournament.dto.request;


import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ClubMemberCreateRequest(
    UUID personId,
    UUID userId,
    ClubMemberRole role,
    ClubMemberStatus status,
    LocalDate joinedAt,
    Boolean student,
    Boolean attendanceViewEnabled,
    String paymentNote,
    String memberNote
) {
}
