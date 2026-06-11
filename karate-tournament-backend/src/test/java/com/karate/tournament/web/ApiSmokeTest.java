package com.karate.tournament.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    UUID category = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Senior Kumite -67kg",
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
    UUID tatami = id(postJson("/api/tournaments/" + tournament + "/tatamis", Map.of(
        "tatamiNo", 1,
        "name", "Tatami 1"
    ), 201));

    postJson("/api/tatamis/" + tatami + "/assign-match", Map.of("matchId", matchId.toString()), 200);
    JsonNode timerStarted = postJson("/api/matches/" + matchId + "/events", Map.of("type", "TIMER_START"), 200);
    assertThat(timerStarted.path("kumite").path("timerRunning").asBoolean()).isTrue();
    assertThat(timerStarted.path("kumite").path("timerStartedAt").asText()).isNotBlank();
    Thread.sleep(25);
    JsonNode timerStopped = postJson("/api/matches/" + matchId + "/events", Map.of("type", "TIMER_STOP"), 200);
    assertThat(timerStopped.path("kumite").path("timerRunning").asBoolean()).isFalse();
    assertThat(timerStopped.path("kumite").path("remainingMs").asInt()).isLessThan(180000);

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

    JsonNode overview = getJson("/api/dashboard/tournaments/" + tournament + "/overview");
    assertThat(overview.path("matches").asLong()).isEqualTo(1);
    assertThat(overview.path("completedMatches").asLong()).isEqualTo(1);
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

  private UUID id(JsonNode node) {
    return UUID.fromString(node.path("id").asText());
  }

  private UUID createAthlete(UUID organizationId, String name, String gender, int weightKg) throws Exception {
    UUID person = id(postJson("/api/persons", Map.of("displayName", name, "gender", gender), 201));
    return id(postJson("/api/athletes", Map.of(
        "personId", person.toString(),
        "primaryOrganizationId", organizationId.toString(),
        "weightKg", weightKg
    ), 201));
  }

  private JsonNode unwrap(String content, int expectedStatus) throws Exception {
    JsonNode root = objectMapper.readTree(content);
    assertThat(root.path("success").asBoolean()).isEqualTo(expectedStatus < 400);
    assertThat(root.path("status").asInt()).isEqualTo(expectedStatus);
    assertThat(root.path("code").asText()).isNotBlank();
    return expectedStatus < 400 ? root.path("data") : root;
  }
}
