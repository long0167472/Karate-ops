package com.karate.tournament.dto.request;

import com.karate.tournament.entity.enums.BeltExamResult;

public record BeltExamCandidateUpdateRequest(
    BeltExamResult result,
    String examinerNote
) {
}
