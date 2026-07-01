package com.karate.tournament.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karate.tournament.auth.AuthenticatedPrincipal;
import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.GlobalAdminCurrentActorProvider;
import com.karate.tournament.entity.CategoryResult;
import com.karate.tournament.entity.enums.SystemRole;
import com.karate.tournament.repository.CategoryResultRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TournamentResolutionApiTest {
  private static final UUID CLUB_MANAGER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  CategoryResultRepository categoryResults;

  @Test
  void tournamentResolutionAwardsPlacementsAndExportsCsv() throws Exception {
    UUID organization = id(postJson("/api/organizations", Map.of(
        "name", "TLKC",
        "shortName", "TLKC",
        "code", "TLKC",
        "type", "CLUB"
    ), 201));

    UUID athleteOne = createRosteredAthlete(organization, "TLKC Member 097", 52);
    UUID athleteTwo = createRosteredAthlete(organization, "TLKC Member 098", 50);
    UUID athleteThree = createRosteredAthlete(organization, "TLKC Member 099", 54);
    UUID athleteFour = createRosteredAthlete(organization, "TLKC Member 100", 56);

    runAs(CLUB_MANAGER_USER_ID, organization, Set.of(SystemRole.CLUB_MANAGER));
    try {
      UUID tournament = id(postJson("/api/tournaments", Map.of(
          "name", "TLKC Resolution Cup",
          "ownerOrganizationId", organization.toString(),
          "visibility", "PRIVATE"
      ), 201));
      UUID category = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
          "name", "Kumite Open",
          "discipline", "KUMITE",
          "openWeight", true,
          "entryType", "INDIVIDUAL",
          "repechageEnabled", true,
          "matchDurationSeconds", 120
      ), 201));

      postJson("/api/tournaments/" + tournament + "/open-registration", Map.of(), 200);
      JsonNode registration = postJson("/api/tournaments/" + tournament + "/registration", Map.of(
          "displayName", "TLKC Delegation"
      ), 201);
      UUID participant = UUID.fromString(registration.path("participantId").asText());

      postJson("/api/tournaments/" + tournament + "/registration/athletes", Map.of(
          "categoryId", category.toString(),
          "athleteId", athleteOne.toString(),
          "registrationWeightKg", 52
      ), 201);
      postJson("/api/tournaments/" + tournament + "/registration/athletes", Map.of(
          "categoryId", category.toString(),
          "athleteId", athleteTwo.toString(),
          "registrationWeightKg", 50
      ), 201);
      postJson("/api/tournaments/" + tournament + "/registration/athletes", Map.of(
          "categoryId", category.toString(),
          "athleteId", athleteThree.toString(),
          "registrationWeightKg", 54
      ), 201);
      JsonNode registrationAfterAdd = postJson("/api/tournaments/" + tournament + "/registration/athletes", Map.of(
          "categoryId", category.toString(),
          "athleteId", athleteFour.toString(),
          "registrationWeightKg", 56
      ), 201);
      assertThat(registrationAfterAdd.path("entries")).hasSize(4);

      JsonNode participantApproval = patchJson("/api/tournaments/" + tournament + "/approval/clubs/" + participant, Map.of(
          "action", "APPROVE",
          "reason", "Club approved for tournament"
      ), 200);
      assertThat(participantApproval.path("status").asText()).isEqualTo("APPROVED");

      JsonNode pendingAthletes = getJson("/api/tournaments/" + tournament + "/approval/athletes?btcStatus=PENDING");
      assertThat(pendingAthletes).hasSize(4);
      for (JsonNode entry : pendingAthletes) {
        patchJson("/api/tournaments/" + tournament + "/approval/athletes/" + entry.path("entryId").asText(), Map.of(
            "action", "APPROVE",
            "reason", "Athlete approved for bracket"
        ), 200);
      }
      JsonNode approvalSummary = getJson("/api/tournaments/" + tournament + "/approval/athletes/summary");
      assertThat(approvalSummary.path("approved").asInt()).isEqualTo(4);
      assertThat(approvalSummary.path("pending").asInt()).isZero();

      postJson("/api/tournaments/" + tournament + "/close-registration", Map.of(), 200);
      postJson("/api/tournaments/" + tournament + "/advance-step", Map.of(), 200);
      postJson("/api/tournaments/" + tournament + "/advance-step", Map.of(), 200);

      JsonNode draw = postJson("/api/tournaments/" + tournament + "/draw", Map.of(), 201);
      assertThat(draw.path("categories")).hasSize(1);
      assertThat(draw.path("categories").get(0).path("athleteCount").asInt()).isEqualTo(4);
      assertThat(draw.path("categories").get(0).path("hasActiveDraw").asBoolean()).isTrue();

      postJson("/api/tournaments/" + tournament + "/draw/start", Map.of(), 204);

      JsonNode matches = getJson("/api/tournaments/" + tournament + "/matches");
      assertThat(matches).hasSize(5);

      JsonNode semifinalOne = matchByNumber(matches, 1);
      JsonNode semifinalTwo = matchByNumber(matches, 2);
      JsonNode semifinalOneAka = participantBySide(semifinalOne, "AKA");
      JsonNode semifinalOneAo = participantBySide(semifinalOne, "AO");
      JsonNode semifinalTwoAka = participantBySide(semifinalTwo, "AKA");
      JsonNode semifinalTwoAo = participantBySide(semifinalTwo, "AO");

      postJson("/api/matches/" + semifinalOne.path("id").asText() + "/events", Map.of(
          "type", "SCORE_DELTA",
          "side", "AKA",
          "points", 3
      ), 200);
      JsonNode semifinalOneResult = postJson("/api/matches/" + semifinalOne.path("id").asText() + "/result", Map.of(
          "winnerSide", "AKA",
          "winType", "POINTS",
          "reason", "Semifinal 1 winner"
      ), 200);
      assertThat(semifinalOneResult.path("status").asText()).isEqualTo("LOCKED");
      assertThat(semifinalOneResult.path("winnerEntryId").asText()).isEqualTo(semifinalOneAka.path("entryId").asText());

      postJson("/api/matches/" + semifinalTwo.path("id").asText() + "/events", Map.of(
          "type", "SCORE_DELTA",
          "side", "AO",
          "points", 3
      ), 200);
      JsonNode semifinalTwoResult = postJson("/api/matches/" + semifinalTwo.path("id").asText() + "/result", Map.of(
          "winnerSide", "AO",
          "winType", "POINTS",
          "reason", "Semifinal 2 winner"
      ), 200);
      assertThat(semifinalTwoResult.path("status").asText()).isEqualTo("LOCKED");
      assertThat(semifinalTwoResult.path("winnerEntryId").asText()).isEqualTo(semifinalTwoAo.path("entryId").asText());

      JsonNode updatedMatches = getJson("/api/tournaments/" + tournament + "/matches");
      JsonNode finalMatch = matchByNumber(updatedMatches, 3);
      JsonNode bronzeOne = matchByNumber(updatedMatches, 4);
      JsonNode bronzeTwo = matchByNumber(updatedMatches, 5);
      JsonNode finalAka = participantBySide(finalMatch, "AKA");
      JsonNode finalAo = participantBySide(finalMatch, "AO");

      assertThat(finalMatch.path("participants")).hasSize(2);
      assertThat(bronzeOne.path("participants")).hasSize(1);
      assertThat(bronzeTwo.path("participants")).hasSize(1);
      assertThat(List.of(finalAka.path("entryId").asText(), finalAo.path("entryId").asText()))
          .containsExactlyInAnyOrder(semifinalOneAka.path("entryId").asText(), semifinalTwoAo.path("entryId").asText());

      JsonNode bronzeOneResult = postJson("/api/matches/" + bronzeOne.path("id").asText() + "/result", Map.of(
          "winnerSide", "AKA",
          "winType", "BYE",
          "reason", "Bronze medal allocation 1"
      ), 200);
      JsonNode bronzeTwoResult = postJson("/api/matches/" + bronzeTwo.path("id").asText() + "/result", Map.of(
          "winnerSide", "AKA",
          "winType", "BYE",
          "reason", "Bronze medal allocation 2"
      ), 200);
      assertThat(bronzeOneResult.path("winnerEntryId").asText()).isEqualTo(participantBySide(bronzeOne, "AKA").path("entryId").asText());
      assertThat(bronzeTwoResult.path("winnerEntryId").asText()).isEqualTo(participantBySide(bronzeTwo, "AKA").path("entryId").asText());

      postJson("/api/matches/" + finalMatch.path("id").asText() + "/events", Map.of(
          "type", "SCORE_DELTA",
          "side", "AO",
          "points", 3
      ), 200);
      JsonNode finalResult = postJson("/api/matches/" + finalMatch.path("id").asText() + "/result", Map.of(
          "winnerSide", "AO",
          "winType", "POINTS",
          "reason", "Final winner"
      ), 200);
      assertThat(finalResult.path("winnerEntryId").asText()).isEqualTo(finalAo.path("entryId").asText());
      assertThat(finalResult.path("status").asText()).isEqualTo("LOCKED");

      List<CategoryResult> placements = categoryResults.findAll().stream()
          .filter(row -> row.category.id.equals(category))
          .sorted(Comparator.comparing(result -> result.placement))
          .toList();
      assertThat(placements).hasSize(4);
      assertThat(placements).extracting(result -> result.medal).containsExactly("GOLD", "SILVER", "BRONZE", "BRONZE");
      assertThat(placements.get(0).entry.id.toString()).isEqualTo(finalAo.path("entryId").asText());
      assertThat(placements.get(1).entry.id.toString()).isEqualTo(finalAka.path("entryId").asText());

      JsonNode clubStandings = getJson("/api/tournaments/" + tournament + "/standings/clubs");
      assertThat(clubStandings).hasSize(1);
      assertThat(clubStandings.get(0).path("organizationName").asText()).isEqualTo("TLKC");
      assertThat(clubStandings.get(0).path("goldMedals").asInt()).isEqualTo(1);
      assertThat(clubStandings.get(0).path("silverMedals").asInt()).isEqualTo(1);
      assertThat(clubStandings.get(0).path("bronzeMedals").asInt()).isEqualTo(2);

      JsonNode athleteStandings = getJson("/api/tournaments/" + tournament + "/standings/athletes");
      assertThat(athleteStandings).hasSize(4);
      assertThat(athleteStandings.findValuesAsText("athleteName"))
          .contains("TLKC Member 097", "TLKC Member 098", "TLKC Member 099", "TLKC Member 100");

      JsonNode entriesExport = getJson("/api/tournaments/" + tournament + "/exports/entries.csv");
      JsonNode scheduleExport = getJson("/api/tournaments/" + tournament + "/exports/schedule.csv");
      JsonNode medalsExport = getJson("/api/tournaments/" + tournament + "/exports/medals.csv");
      assertThat(entriesExport.path("filename").asText()).endsWith("-entries.csv");
      assertThat(entriesExport.path("contentType").asText()).isEqualTo("text/csv");
      assertThat(entriesExport.path("content").asText()).contains("category,delegation,athlete,team,weight_kg,weigh_in_status,status");
      assertThat(entriesExport.path("content").asText()).contains("TLKC Member 097", "TLKC Member 098", "TLKC Member 099", "TLKC Member 100");
      assertThat(scheduleExport.path("filename").asText()).endsWith("-schedule.csv");
      assertThat(scheduleExport.path("contentType").asText()).isEqualTo("text/csv");
      assertThat(scheduleExport.path("content").asText()).contains("match_no,tatami,category,round,position,status,mode,scheduled_at");
      assertThat(scheduleExport.path("content").asText()).contains("\"1\"", "\"2\"", "\"3\"", "\"4\"", "\"5\"");
      assertThat(scheduleExport.path("content").asText()).contains("\"LOCKED\"");
      assertThat(medalsExport.path("filename").asText()).endsWith("-medals.csv");
      assertThat(medalsExport.path("contentType").asText()).isEqualTo("text/csv");
      assertThat(medalsExport.path("content").asText()).contains("organization,gold,silver,bronze,total");
      assertThat(medalsExport.path("content").asText()).contains("\"TLKC\",\"1\",\"1\",\"2\",\"4\"");
    } finally {
      GlobalAdminCurrentActorProvider.clearTestActor();
      SecurityContextHolder.clearContext();
    }
  }

  private UUID createRosteredAthlete(UUID organizationId, String displayName, int weightKg) throws Exception {
    UUID person = id(postJson("/api/persons", Map.of(
        "displayName", displayName,
        "gender", "MALE"
    ), 201));
    postJson("/api/organizations/" + organizationId + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    JsonNode athlete = postJson("/api/organizations/" + organizationId + "/athletes", Map.of(
        "personId", person.toString(),
        "weightKg", weightKg,
        "heightCm", 160,
        "belt", "BLUE",
        "status", "ACTIVE"
    ), 201);
    postJson("/api/organizations/" + organizationId + "/roster", Map.of(
        "athleteId", athlete.path("id").asText(),
        "status", "ACTIVE"
    ), 201);
    return UUID.fromString(athlete.path("id").asText());
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

  private JsonNode patchJson(String url, Object body, int expectedStatus) throws Exception {
    String content = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().is(expectedStatus))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private void runAs(UUID userId, UUID organizationId, Set<SystemRole> roles) {
    GlobalAdminCurrentActorProvider.setTestActor(new CurrentActor(userId, organizationId, roles));
    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, organizationId, "club@test.local", "Club Manager", roles);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, List.of())
    );
  }

  private UUID id(JsonNode node) {
    return UUID.fromString(node.path("id").asText());
  }

  private JsonNode unwrap(String content, int expectedStatus) throws Exception {
    if (content.isBlank()) {
      return objectMapper.createObjectNode();
    }
    JsonNode root = objectMapper.readTree(content);
    assertThat(root.path("success").asBoolean()).isEqualTo(expectedStatus < 400);
    assertThat(root.path("status").asInt()).isEqualTo(expectedStatus);
    return expectedStatus < 400 ? root.path("data") : root;
  }

  private JsonNode matchByNumber(JsonNode matches, int matchNumber) {
    for (JsonNode match : matches) {
      if (match.path("matchNumber").asInt() == matchNumber) {
        return match;
      }
    }
    throw new AssertionError("Missing match number " + matchNumber);
  }

  private JsonNode participantBySide(JsonNode match, String side) {
    for (JsonNode participant : match.path("participants")) {
      if (side.equals(participant.path("side").asText())) {
        return participant;
      }
    }
    throw new AssertionError("Missing participant side " + side + " for match " + match.path("matchNumber").asInt());
  }
}
