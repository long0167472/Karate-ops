package com.karate.tournament.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class BeltExamApiTest {
  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void fullBeltExamLifecyclePassesAndAppliesBelt() throws Exception {
    UUID org = id(postJson("/api/organizations", Map.of("name", "Dan Test Club", "type", "CLUB"), 201));
    UUID person = id(postJson("/api/persons", Map.of("displayName", "Nguyen Thi Dan"), 201));
    UUID athlete = id(postJson("/api/athletes", Map.of(
        "personId", person.toString(),
        "belt", "WHITE"
    ), 201));
    postJson("/api/organizations/" + org + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);

    JsonNode exam = postJson("/api/organizations/" + org + "/belt-exams", Map.of(
        "name", "Kỳ thi lên đai tháng 6",
        "status", "OPEN",
        "examDate", "2026-06-20",
        "location", "Võ đường Q1",
        "examinerName", "HLV Trần Văn A"
    ), 201);
    UUID examId = id(exam);
    assertThat(exam.path("name").asText()).isEqualTo("Kỳ thi lên đai tháng 6");
    assertThat(exam.path("status").asText()).isEqualTo("OPEN");
    assertThat(exam.path("candidates")).hasSize(0);

    JsonNode candidate = postJson("/api/belt-exams/" + examId + "/candidates", Map.of(
        "athleteId", athlete.toString(),
        "currentBelt", "WHITE",
        "targetBelt", "ORANGE"
    ), 201);
    UUID candidateId = id(candidate);
    assertThat(candidate.path("currentBelt").asText()).isEqualTo("WHITE");
    assertThat(candidate.path("targetBelt").asText()).isEqualTo("ORANGE");
    assertThat(candidate.path("result").asText()).isEqualTo("PENDING");
    assertThat(candidate.path("beltApplied").asBoolean()).isFalse();

    postJson("/api/belt-exams/" + examId + "/candidates", Map.of(
        "athleteId", athlete.toString(),
        "currentBelt", "WHITE",
        "targetBelt", "ORANGE"
    ), 409);

    JsonNode examWithCandidate = getJson("/api/belt-exams/" + examId);
    assertThat(examWithCandidate.path("candidates")).hasSize(1);

    JsonNode updatedCandidate = patchJson(
        "/api/belt-exams/" + examId + "/candidates/" + candidateId,
        Map.of("result", "PASS", "examinerNote", "Kỹ thuật tốt"),
        200
    );
    assertThat(updatedCandidate.path("result").asText()).isEqualTo("PASS");
    assertThat(updatedCandidate.path("examinerNote").asText()).isEqualTo("Kỹ thuật tốt");

    patchJson("/api/belt-exams/" + examId, Map.of("status", "COMPLETED"), 200);

    postJson("/api/belt-exams/" + examId + "/apply-results", null, 200);

    JsonNode appliedExam = getJson("/api/belt-exams/" + examId);
    assertThat(appliedExam.path("candidates").get(0).path("beltApplied").asBoolean()).isTrue();

    JsonNode updatedAthlete = getJson("/api/athletes/" + athlete);
    assertThat(updatedAthlete.path("belt").asText()).isEqualTo("ORANGE");

    JsonNode listExams = getJson("/api/organizations/" + org + "/belt-exams");
    assertThat(listExams).hasSize(1);
    assertThat(listExams.get(0).path("id").asText()).isEqualTo(examId.toString());
  }

  @Test
  void cannotModifyCompletedExam() throws Exception {
    UUID org = id(postJson("/api/organizations", Map.of("name", "Completed Exam Club", "type", "CLUB"), 201));
    JsonNode exam = postJson("/api/organizations/" + org + "/belt-exams", Map.of(
        "name", "Locked Exam",
        "status", "COMPLETED"
    ), 201);
    UUID examId = id(exam);

    patchJson("/api/belt-exams/" + examId, Map.of("name", "New Name"), 409);

    mvc.perform(delete("/api/belt-exams/" + examId))
        .andExpect(status().isConflict());
  }

  @Test
  void cannotAddCandidateToCancelledExam() throws Exception {
    UUID org = id(postJson("/api/organizations", Map.of("name", "Cancelled Exam Club", "type", "CLUB"), 201));
    UUID person = id(postJson("/api/persons", Map.of("displayName", "Tran Van B"), 201));
    UUID athlete = id(postJson("/api/athletes", Map.of("personId", person.toString()), 201));
    postJson("/api/organizations/" + org + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);

    JsonNode exam = postJson("/api/organizations/" + org + "/belt-exams", Map.of(
        "name", "Cancelled Exam",
        "status", "CANCELLED"
    ), 201);
    UUID examId = id(exam);

    postJson("/api/belt-exams/" + examId + "/candidates", Map.of(
        "athleteId", athlete.toString(),
        "targetBelt", "ORANGE"
    ), 409);
  }

  @Test
  void applyResultsRequiresCompletedStatus() throws Exception {
    UUID org = id(postJson("/api/organizations", Map.of("name", "Not Completed Club", "type", "CLUB"), 201));
    JsonNode exam = postJson("/api/organizations/" + org + "/belt-exams", Map.of(
        "name", "In Progress Exam",
        "status", "IN_PROGRESS"
    ), 201);
    UUID examId = id(exam);

    postJson("/api/belt-exams/" + examId + "/apply-results", null, 409);
  }

  @Test
  void removeCandidateFromOpenExam() throws Exception {
    UUID org = id(postJson("/api/organizations", Map.of("name", "Remove Candidate Club", "type", "CLUB"), 201));
    UUID person = id(postJson("/api/persons", Map.of("displayName", "Le Van C"), 201));
    UUID athlete = id(postJson("/api/athletes", Map.of("personId", person.toString()), 201));
    postJson("/api/organizations/" + org + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);

    JsonNode exam = postJson("/api/organizations/" + org + "/belt-exams", Map.of(
        "name", "Open Exam",
        "status", "OPEN"
    ), 201);
    UUID examId = id(exam);

    JsonNode candidate = postJson("/api/belt-exams/" + examId + "/candidates", Map.of(
        "athleteId", athlete.toString(),
        "targetBelt", "ORANGE"
    ), 201);
    UUID candidateId = id(candidate);

    mvc.perform(delete("/api/belt-exams/" + examId + "/candidates/" + candidateId))
        .andExpect(status().isNoContent());

    JsonNode examAfterRemove = getJson("/api/belt-exams/" + examId);
    assertThat(examAfterRemove.path("candidates")).hasSize(0);
  }

  private JsonNode postJson(String url, Object body, int expectedStatus) throws Exception {
    var builder = post(url).contentType(MediaType.APPLICATION_JSON);
    if (body != null) builder = builder.content(objectMapper.writeValueAsString(body));
    String content = mvc.perform(builder)
        .andExpect(status().is(expectedStatus))
        .andReturn().getResponse().getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private JsonNode patchJson(String url, Object body, int expectedStatus) throws Exception {
    String content = mvc.perform(patch(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().is(expectedStatus))
        .andReturn().getResponse().getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private JsonNode getJson(String url) throws Exception {
    String content = mvc.perform(get(url))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return unwrap(content, 200);
  }

  private UUID id(JsonNode node) {
    return UUID.fromString(node.path("id").asText());
  }

  private JsonNode unwrap(String content, int expectedStatus) throws Exception {
    if (content.isBlank()) return objectMapper.createObjectNode();
    JsonNode root = objectMapper.readTree(content);
    assertThat(root.path("success").asBoolean()).isEqualTo(expectedStatus < 400);
    assertThat(root.path("status").asInt()).isEqualTo(expectedStatus);
    return expectedStatus < 400 ? root.path("data") : root;
  }
}
