package com.karate.tournament.dto.response;

import com.karate.tournament.entity.enums.BeltExamResult;
import java.math.BigDecimal;
import java.util.List;
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
    boolean beltApplied,
    BigDecimal totalScore,
    BigDecimal maxTotalScore,
    List<BeltExamScoreResponse> scores
) {
}
