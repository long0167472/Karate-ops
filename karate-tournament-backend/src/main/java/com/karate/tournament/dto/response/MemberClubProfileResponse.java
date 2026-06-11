package com.karate.tournament.dto.response;

import java.util.List;

public record MemberClubProfileResponse(
    List<ClubMemberResponse> memberships
) {
}
