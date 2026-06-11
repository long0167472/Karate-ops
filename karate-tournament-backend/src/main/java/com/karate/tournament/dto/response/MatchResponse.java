package com.karate.tournament.dto.response;


import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.WinType;

import com.karate.tournament.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record MatchResponse(
    UUID id,
    UUID tournamentId,
    UUID categoryId,
    String categoryName,
    UUID tatamiId,
    Integer tatamiNo,
    Integer matchNumber,
    String roundName,
    Integer roundNumber,
    Integer bracketPosition,
    MatchStatus status,
    Instant scheduledAt,
    CategoryDiscipline mode,
    UUID winnerEntryId,
    UUID winnerAthleteId,
    WinType winType,
    List<MatchParticipantResponse> participants,
    KumiteStateResponse kumite,
    List<KataVoteResponse> kataVotes,
    List<MatchEventResponse> recentEvents
) {
}
