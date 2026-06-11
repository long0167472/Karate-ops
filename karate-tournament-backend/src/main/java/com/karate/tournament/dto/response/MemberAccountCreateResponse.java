package com.karate.tournament.dto.response;

public record MemberAccountCreateResponse(
    ClubMemberResponse member,
    String username,
    String temporaryPassword
) {
}
