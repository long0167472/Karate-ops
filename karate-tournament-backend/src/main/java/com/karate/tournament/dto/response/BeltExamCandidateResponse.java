package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.BeltExamResult;
import java.util.UUID;

public record BeltExamCandidateResponse(
    UUID id,
    UUID examId,
    UUID organizationMemberId,
    UUID athleteId,
    UUID personId,
    String displayName,
    String currentBelt,
    String targetBelt,
    BeltExamResult result,
    String examinerNote,
    boolean beltApplied
) {
}
