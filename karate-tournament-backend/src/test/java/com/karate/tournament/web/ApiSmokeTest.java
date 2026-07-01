package com.karate.tournament.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.enums.BracketType;
import com.karate.tournament.entity.enums.EntryStatus;
import com.karate.tournament.repository.BracketRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeTest {
  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  MatchRepository matchRepository;

  @Autowired
  BracketRepository bracketRepository;

  @Autowired
  EntryRepository entryRepository;

  @Autowired
  KumiteMatchStateRepository kumiteStates;

  @Test
  void createsTournamentDrawsMatchAssignsTatamiAndStoresWinnerAthlete() throws Exception {
    UUID orgA = id(postJson("/api/organizations", Map.of("name", "Red Club", "type", "CLUB"), 201));
    UUID orgB = id(postJson("/api/organizations", Map.of("name", "Blue Club", "type", "CLUB"), 201));
    UUID tournament = id(postJson("/api/tournaments", Map.of(
        "name", "Open Karate Cup",
        "ownerOrganizationId", orgA.toString(),
        "visibility", "PUBLIC"
    ), 201));

    UUID participantA = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of(
        "organizationId", orgA.toString(),
        "status", "APPROVED"
    ), 201));
    UUID participantB = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of(
        "organizationId", orgB.toString(),
        "status", "APPROVED"
    ), 201));

    UUID personA = id(postJson("/api/persons", Map.of("displayName", "Nguyen Van Aka", "gender", "MALE"), 201));
    UUID personB = id(postJson("/api/persons", Map.of("displayName", "Tran Van Ao", "gender", "MALE"), 201));
    UUID athleteA = id(postJson("/api/athletes", Map.of(
        "personId", personA.toString(),
        "primaryOrganizationId", orgA.toString()
    ), 201));
    UUID athleteB = id(postJson("/api/athletes", Map.of(
        "personId", personB.toString(),
        "primaryOrganizationId", orgB.toString()
    ), 201));
    postJson("/api/organizations/" + orgA + "/members", Map.of(
        "personId", personA.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    postJson("/api/organizations/" + orgB + "/members", Map.of(
        "personId", personB.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    postJson("/api/organizations/" + orgA + "/roster", Map.of("athleteId", athleteA.toString(), "status", "ACTIVE"), 201);
    postJson("/api/organizations/" + orgB + "/roster", Map.of("athleteId", athleteB.toString(), "status", "ACTIVE"), 201);

    UUID category = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Kumite -67kg",
        "discipline", "KUMITE",
        "gender", "MALE",
        "matchDurationSeconds", 120
    ), 201));
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantA.toString(),
        "athleteId", athleteA.toString(),
        "seedNo", 1
    ), 201);
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantB.toString(),
        "athleteId", athleteB.toString(),
        "seedNo", 2
    ), 201);

    JsonNode draw = postJson("/api/categories/" + category + "/draw", Map.of("shuffle", false), 200);
    UUID matchId = UUID.fromString(draw.path("matches").get(0).path("id").asText());
    UUID tatami = id(postJson("/api/tournaments/" + tournament + "/tatamis", Map.of(
        "tatamiNo", 1,
        "name", "Tatami 1"
    ), 201));

    postJson("/api/tatamis/" + tatami + "/assign-match", Map.of("matchId", matchId.toString()), 200);
    JsonNode initialMatch = getJson("/api/matches/" + matchId);
    assertThat(initialMatch.path("kumite").path("durationMs").asInt()).isEqualTo(120000);
    assertThat(initialMatch.path("kumite").path("remainingMs").asInt()).isEqualTo(120000);
    JsonNode timerStarted = postJson("/api/matches/" + matchId + "/events", Map.of("type", "TIMER_START"), 200);
    assertThat(timerStarted.path("kumite").path("timerRunning").asBoolean()).isTrue();
    assertThat(timerStarted.path("kumite").path("timerStartedAt").asText()).isNotBlank();
    Thread.sleep(25);
    JsonNode timerStopped = postJson("/api/matches/" + matchId + "/events", Map.of("type", "TIMER_STOP"), 200);
    assertThat(timerStopped.path("kumite").path("timerRunning").asBoolean()).isFalse();
    assertThat(timerStopped.path("kumite").path("remainingMs").asInt()).isLessThan(120000);

    postJson("/api/matches/" + matchId + "/events", Map.of(
        "type", "SCORE_DELTA",
        "side", "AKA",
        "points", 3
    ), 200);
    JsonNode result = postJson("/api/matches/" + matchId + "/result", Map.of(
        "winnerSide", "AKA",
        "winType", "POINTS"
    ), 200);

    assertThat(result.path("winnerAthleteId").asText()).isEqualTo(athleteA.toString());
    assertThat(result.path("status").asText()).isEqualTo("LOCKED");

    mvc.perform(get("/api/tatamis/" + tatami + "/current-match"))
        .andExpect(status().isNoContent());

    mvc.perform(post("/api/matches/" + matchId + "/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "type", "SCORE_DELTA",
                "side", "AO",
                "points", 1
            ))))
        .andExpect(status().isConflict());

    JsonNode overview = getJson("/api/dashboard/tournaments/" + tournament + "/overview");
    assertThat(overview.path("matches").asLong()).isEqualTo(1);
    assertThat(overview.path("completedMatches").asLong()).isEqualTo(1);
  }

  @Test
  void confirmResultInfersKikenWhenWinnerSideMatchesRuleSuggestion() throws Exception {
    UUID orgA = id(postJson("/api/organizations", Map.of("name", "Kiken Red", "type", "CLUB"), 201));
    UUID orgB = id(postJson("/api/organizations", Map.of("name", "Kiken Blue", "type", "CLUB"), 201));
    UUID tournament = id(postJson("/api/tournaments", Map.of(
        "name", "Kiken Cup",
        "ownerOrganizationId", orgA.toString(),
        "visibility", "PUBLIC"
    ), 201));

    UUID participantA = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of(
        "organizationId", orgA.toString(),
        "status", "APPROVED"
    ), 201));
    UUID participantB = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of(
        "organizationId", orgB.toString(),
        "status", "APPROVED"
    ), 201));
    UUID athleteA = createAthlete(orgA, "Kiken Aka", "MALE", 60);
    UUID athleteB = createAthlete(orgB, "Kiken Ao", "MALE", 60);

    UUID category = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Kumite Open",
        "discipline", "KUMITE",
        "gender", "MALE"
    ), 201));
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantA.toString(),
        "athleteId", athleteA.toString(),
        "seedNo", 1
    ), 201);
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantB.toString(),
        "athleteId", athleteB.toString(),
        "seedNo", 2
    ), 201);

    JsonNode draw = postJson("/api/categories/" + category + "/draw", Map.of("shuffle", false), 200);
    UUID matchId = UUID.fromString(draw.path("matches").get(0).path("id").asText());

    postJson("/api/matches/" + matchId + "/events", Map.of(
        "type", "PENALTY",
        "side", "AO",
        "penaltyCode", "KIKEN"
    ), 200);
    JsonNode result = postJson("/api/matches/" + matchId + "/result", Map.of(
        "winnerSide", "AKA"
    ), 200);

    assertThat(result.path("status").asText()).isEqualTo("LOCKED");
    assertThat(result.path("winType").asText()).isEqualTo("KIKEN");
  }

  @Test
  void wkf2026PenaltyLadderUsesSingleChuiSequenceWithLegacyCompatibilityFields() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("PenaltyLadder");

    JsonNode chui1 = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AKA",
        "penaltyLevel", "CHUI_1",
        "penaltyReasonCode", "JOGAI"
    ), 200);
    assertThat(chui1.path("kumite").path("penalties").path("aka").path("penaltyLevel").asText()).isEqualTo("CHUI_1");
    assertThat(chui1.path("kumite").path("penalties").path("aka").path("reasonCode").asText()).isEqualTo("JOGAI");
    assertThat(chui1.path("kumite").path("akaChui").asInt()).isEqualTo(1);

    JsonNode chui3 = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AKA",
        "penaltyLevel", "CHUI_3",
        "penaltyReasonCode", "GRABBING"
    ), 200);
    assertThat(chui3.path("kumite").path("penalties").path("aka").path("penaltyLevel").asText()).isEqualTo("CHUI_3");
    assertThat(chui3.path("kumite").path("akaChui").asInt()).isEqualTo(3);
    assertThat(chui3.path("kumite").path("akaHansokuChui").asBoolean()).isFalse();

    JsonNode hc = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AKA",
        "penaltyLevel", "HANSOKU_CHUI",
        "penaltyReasonCode", "MUBOBI"
    ), 200);
    assertThat(hc.path("kumite").path("penalties").path("aka").path("penaltyLevel").asText()).isEqualTo("HANSOKU_CHUI");
    assertThat(hc.path("kumite").path("akaChui").asInt()).isEqualTo(3);
    assertThat(hc.path("kumite").path("akaHansokuChui").asBoolean()).isTrue();
    assertThat(hc.path("kumite").path("penalties").path("aka").path("category1Level").asText()).isEqualTo("HANSOKU_CHUI");
    assertThat(hc.path("kumite").path("penalties").path("aka").path("category2Level").asText()).isEqualTo("NONE");
  }

  @Test
  void hansokuPenaltyFreezesOpponentDecisionAndConfirmInfersHansoku() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("Hansoku");

    JsonNode pending = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AKA",
        "penaltyLevel", "HANSOKU",
        "penaltyReasonCode", "EXCESSIVE_CONTACT"
    ), 200);

    assertThat(pending.path("status").asText()).isEqualTo("RESULT_PENDING_CONFIRMATION");
    assertThat(pending.path("kumite").path("decision").path("winnerSide").asText()).isEqualTo("AO");
    assertThat(pending.path("kumite").path("decision").path("winType").asText()).isEqualTo("HANSOKU");

    JsonNode result = postJson("/api/matches/" + fixture.matchId() + "/result", Map.of(
        "winnerSide", "AO"
    ), 200);
    assertThat(result.path("status").asText()).isEqualTo("LOCKED");
    assertThat(result.path("winType").asText()).isEqualTo("HANSOKU");
  }

  @Test
  void shikkakuDirectPenaltyFreezesOpponentDecision() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("Shikkaku");

    JsonNode pending = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AO",
        "penaltyCode", "SHIKKAKU"
    ), 200);

    assertThat(pending.path("status").asText()).isEqualTo("RESULT_PENDING_CONFIRMATION");
    assertThat(pending.path("kumite").path("aoShikkaku").asBoolean()).isTrue();
    assertThat(pending.path("kumite").path("decision").path("winnerSide").asText()).isEqualTo("AKA");
    assertThat(pending.path("kumite").path("decision").path("winType").asText()).isEqualTo("SHIKKAKU");
  }

  @Test
  void passivityIsRejectedInFirstAndFinalFifteenSeconds() throws Exception {
    MatchFixture firstWindow = createSimpleKumiteFixture("PassivityFirst");
    postJson("/api/matches/" + firstWindow.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AKA",
        "penaltyLevel", "CHUI_1",
        "penaltyReasonCode", "PASSIVITY"
    ), 400);

    MatchFixture finalWindow = createSimpleKumiteFixture("PassivityFinal");
    postJson("/api/matches/" + finalWindow.matchId() + "/events", Map.of(
        "type", "TIMER_SET",
        "timerMs", 15000
    ), 200);
    postJson("/api/matches/" + finalWindow.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AO",
        "penaltyLevel", "CHUI_1",
        "penaltyReasonCode", "PASSIVITY"
    ), 400);
  }

  @Test
  void avoidingCombatInFinalFifteenPromotesToHansokuChuiAndRevokesOffenderSenshu() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("AvoidingFinal");

    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_DELTA",
        "side", "AKA",
        "points", 1
    ), 200);
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "TIMER_SET",
        "timerMs", 15000
    ), 200);
    JsonNode penalized = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "PENALTY",
        "side", "AKA",
        "penaltyLevel", "CHUI_1",
        "penaltyReasonCode", "AVOIDING_COMBAT"
    ), 200);

    assertThat(penalized.path("kumite").path("penalties").path("aka").path("penaltyLevel").asText()).isEqualTo("HANSOKU_CHUI");
    assertThat(penalized.path("kumite").path("akaHansokuChui").asBoolean()).isTrue();
    assertThat(penalized.path("kumite").path("akaSenshu").asBoolean()).isFalse();
    assertThat(penalized.path("kumite").path("senshu").path("holderSide").isMissingNode()
        || penalized.path("kumite").path("senshu").path("holderSide").isNull()).isTrue();
    assertThat(penalized.path("kumite").path("senshu").path("revocationReasonCode").asText()).isEqualTo("AVOIDING_COMBAT");
  }

  @Test
  void hanteiFreezeRequiresDecisionBeforeConfirm() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("Hantei");
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "TIMER_SET",
        "timerMs", 0
    ), 200);

    JsonNode frozen = getJson("/api/matches/" + fixture.matchId());
    assertThat(frozen.path("status").asText()).isEqualTo("HANTEI");

    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "HANTEI_DECISION",
        "side", "AKA"
    ), 200);
    JsonNode result = postJson("/api/matches/" + fixture.matchId() + "/result", Map.of(
        "winnerSide", "AKA"
    ), 200);

    assertThat(result.path("status").asText()).isEqualTo("LOCKED");
    assertThat(result.path("winType").asText()).isEqualTo("HANTEI");
  }

  @Test
  void akaScoresAloneFirstAwardsAkaSenshu() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("SenshuAlone");

    JsonNode scored = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_DELTA",
        "side", "AKA",
        "points", 1
    ), 200);

    assertThat(scored.path("kumite").path("akaScore").asInt()).isEqualTo(1);
    assertThat(scored.path("kumite").path("senshu").path("holderSide").asText()).isEqualTo("AKA");
    assertThat(scored.path("kumite").path("akaSenshu").asBoolean()).isTrue();
    assertThat(scored.path("kumite").path("aoSenshu").asBoolean()).isFalse();
  }

  @Test
  void simultaneousFirstExchangeDoesNotAwardSenshu() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("SenshuSimultaneous");

    JsonNode scored = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_EXCHANGE",
        "exchangeId", "exchange-1",
        "payloadJson", objectMapper.writeValueAsString(Map.of(
            "exchangeId", "exchange-1",
            "akaPoints", 1,
            "aoPoints", 1
        ))
    ), 200);

    assertThat(scored.path("kumite").path("akaScore").asInt()).isEqualTo(1);
    assertThat(scored.path("kumite").path("aoScore").asInt()).isEqualTo(1);
    assertThat(scored.path("kumite").path("senshu").path("holderSide").isMissingNode()
        || scored.path("kumite").path("senshu").path("holderSide").isNull()).isTrue();
    assertThat(scored.path("kumite").path("akaSenshu").asBoolean()).isFalse();
    assertThat(scored.path("kumite").path("aoSenshu").asBoolean()).isFalse();
  }

  @Test
  void afterSimultaneousFirstExchangeLaterAkaScoreAwardsAkaSenshu() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("SenshuLater");

    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_EXCHANGE",
        "exchangeId", "exchange-1",
        "payloadJson", objectMapper.writeValueAsString(Map.of(
            "exchangeId", "exchange-1",
            "akaPoints", 1,
            "aoPoints", 1
        ))
    ), 200);
    JsonNode scored = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_DELTA",
        "side", "AKA",
        "points", 2
    ), 200);

    assertThat(scored.path("kumite").path("akaScore").asInt()).isEqualTo(3);
    assertThat(scored.path("kumite").path("aoScore").asInt()).isEqualTo(1);
    assertThat(scored.path("kumite").path("senshu").path("holderSide").asText()).isEqualTo("AKA");
    assertThat(scored.path("kumite").path("akaSenshu").asBoolean()).isTrue();
  }

  @Test
  void roundRobinKumiteCanFreezeAndConfirmHikiwake() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("Hikiwake");
    Match match = matchRepository.findById(fixture.matchId()).orElseThrow();
    match.bracket.type = BracketType.ROUND_ROBIN;
    bracketRepository.save(match.bracket);

    JsonNode frozen = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "TIMER_SET",
        "timerMs", 0
    ), 200);

    assertThat(frozen.path("status").asText()).isEqualTo("HIKIWAKE");
    assertThat(frozen.path("kumite").path("decision").path("winType").asText()).isEqualTo("HIKIWAKE");
    assertThat(frozen.path("kumite").path("decision").path("winnerSide").isMissingNode()
        || frozen.path("kumite").path("decision").path("winnerSide").isNull()).isTrue();
    assertThat(frozen.path("kumite").path("decision").path("confirmable").asBoolean()).isTrue();

    JsonNode result = postJson("/api/matches/" + fixture.matchId() + "/result", Map.of(
        "winType", "HIKIWAKE"
    ), 200);

    assertThat(result.path("status").asText()).isEqualTo("LOCKED");
    assertThat(result.path("winType").asText()).isEqualTo("HIKIWAKE");
    assertThat(result.path("winnerEntryId").isMissingNode() || result.path("winnerEntryId").isNull()).isTrue();
  }

  @Test
  void deniedVideoReviewConsumesOnlyRequesterCard() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("Review");

    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_REQUEST",
        "side", "AKA"
    ), 200);
    JsonNode resolved = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_RESOLVE",
        "resolution", "DENIED"
    ), 200);

    assertThat(resolved.path("kumite").path("videoReview").path("akaCardAvailable").asBoolean()).isFalse();
    assertThat(resolved.path("kumite").path("videoReview").path("aoCardAvailable").asBoolean()).isTrue();
    JsonNode audit = latestEventPayload(resolved);
    assertThat(audit.path("resolution").asText()).isEqualTo("DENIED");
    assertThat(audit.path("reasonCode").asText()).isEqualTo("");
    assertThat(audit.path("cardConsumed").asBoolean()).isTrue();
  }

  @Test
  void acceptedVideoReviewScoreKeepsRequesterCard() throws Exception {
    JsonNode resolved = resolveVideoReview("ReviewScore", "AWARD_SCORE", Map.of(
        "resolutionSide", "AKA",
        "resolutionPoints", 2,
        "reasonCode", "VALID_SCORE"
    ));

    assertThat(resolved.path("kumite").path("videoReview").path("akaCardAvailable").asBoolean()).isTrue();
    assertThat(resolved.path("kumite").path("videoReview").path("lastResolution").asText()).isEqualTo("AWARD_SCORE");
    assertThat(latestEventPayload(resolved).path("cardKept").asBoolean()).isTrue();
  }

  @Test
  void mienaiVideoReviewKeepsRequesterCard() throws Exception {
    JsonNode resolved = resolveVideoReview("ReviewMienai", "MIENAI", Map.of(
        "reasonCode", "MIENAI",
        "reasonText", "Video does not show the action clearly"
    ));

    assertThat(resolved.path("kumite").path("videoReview").path("akaCardAvailable").asBoolean()).isTrue();
    assertThat(resolved.path("kumite").path("videoReview").path("lastResolution").asText()).isEqualTo("MIENAI");
    assertThat(latestEventPayload(resolved).path("cardKept").asBoolean()).isTrue();
  }

  @Test
  void technicalProblemVideoReviewKeepsRequesterCard() throws Exception {
    JsonNode resolved = resolveVideoReview("ReviewTech", "TECHNICAL_PROBLEM", Map.of(
        "reasonCode", "TECHNICAL_PROBLEM"
    ));

    assertThat(resolved.path("kumite").path("videoReview").path("akaCardAvailable").asBoolean()).isTrue();
    assertThat(resolved.path("kumite").path("videoReview").path("lastResolution").asText()).isEqualTo("TECHNICAL_PROBLEM");
    assertThat(latestEventPayload(resolved).path("cardKept").asBoolean()).isTrue();
  }

  @Test
  void torimasenVideoReviewKeepsRequesterCard() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("ReviewTorimasen");
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_DELTA",
        "side", "AKA",
        "points", 1
    ), 200);
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_REQUEST",
        "side", "AKA"
    ), 200);
    JsonNode resolved = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_RESOLVE",
        "resolution", "TORIMASEN",
        "resolutionSide", "AKA",
        "resolutionPoints", 1,
        "reasonCode", "SCORE_CANCELLED"
    ), 200);

    assertThat(resolved.path("kumite").path("videoReview").path("akaCardAvailable").asBoolean()).isTrue();
    assertThat(resolved.path("kumite").path("videoReview").path("lastResolution").asText()).isEqualTo("TORIMASEN");
    assertThat(latestEventPayload(resolved).path("cardKept").asBoolean()).isTrue();
  }

  @Test
  void revokeSenshuVideoReviewKeepsRequesterCard() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("ReviewSenshu");
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "SCORE_DELTA",
        "side", "AKA",
        "points", 1
    ), 200);
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_REQUEST",
        "side", "AKA"
    ), 200);
    JsonNode resolved = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_RESOLVE",
        "resolution", "REVOKE_SENSHU",
        "resolutionSide", "AKA",
        "reasonCode", "SENSHU_REVIEW"
    ), 200);

    assertThat(resolved.path("kumite").path("videoReview").path("akaCardAvailable").asBoolean()).isTrue();
    assertThat(resolved.path("kumite").path("videoReview").path("lastResolution").asText()).isEqualTo("REVOKE_SENSHU");
    assertThat(latestEventPayload(resolved).path("cardKept").asBoolean()).isTrue();
  }

  @Test
  void medicalTimeoutCanConfirmTenSecondRule() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("Medical");

    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "MEDICAL_START",
        "side", "AO"
    ), 200);
    JsonNode pending = postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "MEDICAL_RESOLVE",
        "medicalOutcome", "UNFIT_TEN_SECOND_RULE"
    ), 200);
    assertThat(pending.path("status").asText()).isEqualTo("RESULT_PENDING_CONFIRMATION");
    assertThat(pending.path("kumite").path("decision").path("winType").asText()).isEqualTo("TEN_SECOND_RULE");

    JsonNode result = postJson("/api/matches/" + fixture.matchId() + "/result", Map.of(
        "winnerSide", "AKA"
    ), 200);
    assertThat(result.path("status").asText()).isEqualTo("LOCKED");
    assertThat(result.path("winType").asText()).isEqualTo("TEN_SECOND_RULE");
  }

  @Test
  void expiredMedicalDeadlineSetsTenSecondDecision() throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture("MedicalDeadline");

    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "MEDICAL_START",
        "side", "AO"
    ), 200);
    var state = kumiteStates.findById(fixture.matchId()).orElseThrow();
    state.medicalDeadlineAt = Instant.now().minusSeconds(1);
    kumiteStates.saveAndFlush(state);

    JsonNode result = postJson("/api/matches/" + fixture.matchId() + "/result", Map.of(
        "winnerSide", "AKA"
    ), 200);

    assertThat(result.path("status").asText()).isEqualTo("LOCKED");
    assertThat(result.path("winType").asText()).isEqualTo("TEN_SECOND_RULE");
    assertThat(result.path("winnerAthleteId").asText()).isEqualTo(fixture.akaAthleteId().toString());
  }

  @Test
  void tenSecondRuleWithdrawsAthleteFromLaterKumiteButNotKata() throws Exception {
    UUID orgA = id(postJson("/api/organizations", Map.of("name", "Ten Rule Red", "type", "CLUB"), 201));
    UUID orgB = id(postJson("/api/organizations", Map.of("name", "Ten Rule Blue", "type", "CLUB"), 201));
    UUID orgC = id(postJson("/api/organizations", Map.of("name", "Ten Rule Green", "type", "CLUB"), 201));
    UUID tournament = id(postJson("/api/tournaments", Map.of(
        "name", "Ten Rule Cup",
        "ownerOrganizationId", orgA.toString(),
        "visibility", "PUBLIC",
        "rulesetPreset", "WKF"
    ), 201));
    UUID participantA = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgA.toString(), "status", "APPROVED"), 201));
    UUID participantB = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgB.toString(), "status", "APPROVED"), 201));
    UUID participantC = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgC.toString(), "status", "APPROVED"), 201));
    UUID athleteA = createAthlete(orgA, "Ten Rule Aka", "MALE", 60);
    UUID athleteB = createAthlete(orgB, "Ten Rule Injured", "MALE", 60);
    UUID athleteC = createAthlete(orgC, "Ten Rule Later Opponent", "MALE", 60);

    UUID firstKumite = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Male Kumite -60",
        "discipline", "KUMITE",
        "gender", "MALE"
    ), 201));
    postJson("/api/categories/" + firstKumite + "/entries", Map.of("tournamentParticipantId", participantA.toString(), "athleteId", athleteA.toString(), "seedNo", 1), 201);
    postJson("/api/categories/" + firstKumite + "/entries", Map.of("tournamentParticipantId", participantB.toString(), "athleteId", athleteB.toString(), "seedNo", 2), 201);
    UUID currentMatch = UUID.fromString(postJson("/api/categories/" + firstKumite + "/draw", Map.of("shuffle", false), 200)
        .path("matches").get(0).path("id").asText());

    UUID laterKumite = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Male Kumite Open",
        "discipline", "KUMITE",
        "gender", "MALE"
    ), 201));
    postJson("/api/categories/" + laterKumite + "/entries", Map.of("tournamentParticipantId", participantC.toString(), "athleteId", athleteC.toString(), "seedNo", 1), 201);
    postJson("/api/categories/" + laterKumite + "/entries", Map.of("tournamentParticipantId", participantB.toString(), "athleteId", athleteB.toString(), "seedNo", 2), 201);
    UUID laterMatch = UUID.fromString(postJson("/api/categories/" + laterKumite + "/draw", Map.of("shuffle", false), 200)
        .path("matches").get(0).path("id").asText());

    UUID kata = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Male Kata",
        "discipline", "KATA",
        "gender", "MALE"
    ), 201));
    postJson("/api/categories/" + kata + "/entries", Map.of("tournamentParticipantId", participantB.toString(), "athleteId", athleteB.toString(), "seedNo", 1), 201);
    int liveEntryCount = entryRepository.findByTournament(tournament).size();

    postJson("/api/matches/" + currentMatch + "/events", Map.of(
        "type", "MEDICAL_START",
        "side", "AO"
    ), 200);
    postJson("/api/matches/" + currentMatch + "/events", Map.of(
        "type", "MEDICAL_RESOLVE",
        "medicalOutcome", "UNFIT_TEN_SECOND_RULE"
    ), 200);

    assertThat(entryRepository.findByCategory_IdAndAthlete_IdAndDeletedAtIsNull(firstKumite, athleteB).orElseThrow().status)
        .isEqualTo(EntryStatus.WITHDRAWN);
    assertThat(entryRepository.findByCategory_IdAndAthlete_IdAndDeletedAtIsNull(laterKumite, athleteB).orElseThrow().status)
        .isEqualTo(EntryStatus.WITHDRAWN);
    assertThat(entryRepository.findByCategory_IdAndAthlete_IdAndDeletedAtIsNull(kata, athleteB).orElseThrow().status)
        .isEqualTo(EntryStatus.REGISTERED);
    assertThat(entryRepository.findByTournament(tournament)).hasSize(liveEntryCount);

    JsonNode laterResult = getJson("/api/matches/" + laterMatch);
    assertThat(laterResult.path("status").asText()).isEqualTo("LOCKED");
    assertThat(laterResult.path("winType").asText()).isEqualTo("TEN_SECOND_RULE");
    assertThat(laterResult.path("winnerAthleteId").asText()).isEqualTo(athleteC.toString());
  }

  @Test
  void softDeletesOrganizationsFromList() throws Exception {
    UUID org = id(postJson("/api/organizations", Map.of("name", "Temporary Club", "type", "CLUB"), 201));

    mvc.perform(delete("/api/organizations/" + org))
        .andExpect(status().isNoContent());

    JsonNode list = getJson("/api/organizations");
    assertThat(list.findValuesAsText("id")).doesNotContain(org.toString());
  }

  @Test
  void wkfTournamentSetupSupportsOpenWeightValidationTeamCategoriesAndRepechage() throws Exception {
    UUID owner = id(postJson("/api/organizations", Map.of("name", "WKF Host", "type", "ORGANIZER"), 201));
    UUID tournament = id(postJson("/api/tournaments", Map.of(
        "name", "WKF Setup Cup",
        "ownerOrganizationId", owner.toString(),
        "organizerName", "National Karate Committee",
        "tatamiCount", 3,
        "competitionLevels", List.of("PHONG_TRAO", "NANG_CAO"),
        "rulesetPreset", "WKF",
        "visibility", "PUBLIC"
    ), 201));

    JsonNode openWeight = postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Male Open Kumite",
        "discipline", "KUMITE",
        "gender", "MALE"
    ), 201);
    assertThat(openWeight.path("openWeight").asBoolean()).isTrue();
    assertThat(openWeight.path("weightLabel").asText()).isEqualTo("Vo dich tuyet doi");

    postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Female Kata",
        "discipline", "KATA",
        "gender", "FEMALE",
        "competitionLevel", "NANG_CAO"
    ), 201);
    postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Team Male Kumite",
        "discipline", "TEAM_KUMITE",
        "entryType", "TEAM",
        "gender", "MALE"
    ), 201);
    postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Team Kata Mixed",
        "discipline", "TEAM_KATA",
        "entryType", "TEAM",
        "gender", "MIXED"
    ), 201);
    JsonNode categoryList = getJson("/api/tournaments/" + tournament + "/categories");
    assertThat(categoryList).hasSize(4);

    UUID weightedCategory = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Male Kumite -67kg",
        "discipline", "KUMITE",
        "gender", "MALE",
        "weightMaxKg", 67,
        "repechageEnabled", true
    ), 201));

    UUID orgA = id(postJson("/api/organizations", Map.of("name", "WKF Red", "type", "CLUB"), 201));
    UUID orgB = id(postJson("/api/organizations", Map.of("name", "WKF Blue", "type", "CLUB"), 201));
    UUID orgC = id(postJson("/api/organizations", Map.of("name", "WKF Green", "type", "CLUB"), 201));
    UUID orgD = id(postJson("/api/organizations", Map.of("name", "WKF Gold", "type", "CLUB"), 201));
    UUID participantA = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgA.toString(), "status", "APPROVED"), 201));
    UUID participantB = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgB.toString(), "status", "APPROVED"), 201));
    UUID participantC = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgC.toString(), "status", "APPROVED"), 201));
    UUID participantD = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgD.toString(), "status", "APPROVED"), 201));

    UUID overweight = createAthlete(orgA, "Over Weight", "MALE", 72);
    postJson("/api/categories/" + weightedCategory + "/entries", Map.of(
        "tournamentParticipantId", participantA.toString(),
        "athleteId", overweight.toString()
    ), 400);

    UUID athleteA = createAthlete(orgA, "Seed One", "MALE", 66);
    UUID athleteB = createAthlete(orgB, "Seed Two", "MALE", 66);
    UUID athleteC = createAthlete(orgC, "Seed Three", "MALE", 66);
    UUID athleteD = createAthlete(orgD, "Seed Four", "MALE", 66);
    postJson("/api/categories/" + weightedCategory + "/entries", Map.of("tournamentParticipantId", participantA.toString(), "athleteId", athleteA.toString()), 201);
    postJson("/api/categories/" + weightedCategory + "/entries", Map.of("tournamentParticipantId", participantB.toString(), "athleteId", athleteB.toString()), 201);
    postJson("/api/categories/" + weightedCategory + "/entries", Map.of("tournamentParticipantId", participantC.toString(), "athleteId", athleteC.toString()), 201);
    postJson("/api/categories/" + weightedCategory + "/entries", Map.of("tournamentParticipantId", participantD.toString(), "athleteId", athleteD.toString()), 201);

    JsonNode draw = postJson("/api/categories/" + weightedCategory + "/draw", Map.of(
        "bracketType", "REPECHAGE",
        "enableRepechage", true,
        "shuffle", false
    ), 200);
    long bronzeMatches = java.util.stream.StreamSupport.stream(draw.path("matches").spliterator(), false)
        .filter(match -> match.path("roundName").asText().startsWith("Bronze Medal"))
        .count();
    assertThat(bronzeMatches).isEqualTo(2);
  }

  private JsonNode postJson(String url, Object body, int expectedStatus) throws Exception {
    String content = mvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().is(expectedStatus))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private JsonNode getJson(String url) throws Exception {
    String content = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return unwrap(content, 200);
  }

  private JsonNode resolveVideoReview(String prefix, String resolution, Map<String, Object> extra) throws Exception {
    MatchFixture fixture = createSimpleKumiteFixture(prefix);
    postJson("/api/matches/" + fixture.matchId() + "/events", Map.of(
        "type", "VIDEO_REVIEW_REQUEST",
        "side", "AKA"
    ), 200);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "VIDEO_REVIEW_RESOLVE");
    body.put("resolution", resolution);
    body.putAll(extra);
    return postJson("/api/matches/" + fixture.matchId() + "/events", body, 200);
  }

  private JsonNode latestEventPayload(JsonNode match) throws Exception {
    JsonNode events = match.path("recentEvents");
    String payloadJson = events.get(events.size() - 1).path("payloadJson").asText();
    return objectMapper.readTree(payloadJson);
  }

  private UUID id(JsonNode node) {
    return UUID.fromString(node.path("id").asText());
  }

  private UUID createAthlete(UUID organizationId, String name, String gender, int weightKg) throws Exception {
    UUID person = id(postJson("/api/persons", Map.of("displayName", name, "gender", gender), 201));
    postJson("/api/organizations/" + organizationId + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    UUID athlete = id(postJson("/api/athletes", Map.of(
        "personId", person.toString(),
        "primaryOrganizationId", organizationId.toString(),
        "weightKg", weightKg
    ), 201));
    postJson("/api/organizations/" + organizationId + "/roster", Map.of("athleteId", athlete.toString(), "status", "ACTIVE"), 201);
    return athlete;
  }

  private MatchFixture createSimpleKumiteFixture(String prefix) throws Exception {
    UUID orgA = id(postJson("/api/organizations", Map.of("name", prefix + " Red", "type", "CLUB"), 201));
    UUID orgB = id(postJson("/api/organizations", Map.of("name", prefix + " Blue", "type", "CLUB"), 201));
    UUID tournament = id(postJson("/api/tournaments", Map.of(
        "name", prefix + " Cup",
        "ownerOrganizationId", orgA.toString(),
        "visibility", "PUBLIC",
        "rulesetPreset", "WKF"
    ), 201));
    UUID participantA = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgA.toString(), "status", "APPROVED"), 201));
    UUID participantB = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of("organizationId", orgB.toString(), "status", "APPROVED"), 201));
    UUID athleteA = createAthlete(orgA, prefix + " Aka", "MALE", 60);
    UUID athleteB = createAthlete(orgB, prefix + " Ao", "MALE", 60);
    UUID category = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", prefix + " Kumite",
        "discipline", "KUMITE",
        "gender", "MALE"
    ), 201));
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantA.toString(),
        "athleteId", athleteA.toString(),
        "seedNo", 1
    ), 201);
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantB.toString(),
        "athleteId", athleteB.toString(),
        "seedNo", 2
    ), 201);
    JsonNode draw = postJson("/api/categories/" + category + "/draw", Map.of("shuffle", false), 200);
    UUID matchId = UUID.fromString(draw.path("matches").get(0).path("id").asText());
    return new MatchFixture(tournament, category, matchId, athleteA, athleteB);
  }

  private JsonNode unwrap(String content, int expectedStatus) throws Exception {
    JsonNode root = objectMapper.readTree(content);
    assertThat(root.path("success").asBoolean()).isEqualTo(expectedStatus < 400);
    assertThat(root.path("status").asInt()).isEqualTo(expectedStatus);
    assertThat(root.path("code").asText()).isNotBlank();
    return expectedStatus < 400 ? root.path("data") : root;
  }

  private record MatchFixture(
      UUID tournamentId,
      UUID categoryId,
      UUID matchId,
      UUID akaAthleteId,
      UUID aoAthleteId
  ) {
  }
}
