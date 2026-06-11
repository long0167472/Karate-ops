package com.karate.tournament.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karate.tournament.auth.AuthenticatedPrincipal;
import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.GlobalAdminCurrentActorProvider;
import com.karate.tournament.entity.enums.SystemRole;
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
class AccountRequestApiTest {
  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void publicRequestApproveAndDirectCreateGenerateLoginAccounts() throws Exception {
    UUID clubId = createClub("Thang Long Karate", "TLKC");

    JsonNode lookup = getJson("/api/public/clubs/lookup?code=tlkc", 200);
    assertThat(lookup.path("code").asText()).isEqualTo("TLKC");

    JsonNode request = postJson("/api/account-requests", Map.of(
        "organizationCode", "TLKC",
        "displayName", "Phạm Thành Long",
        "email", "long.pt@example.test",
        "phone", "0909 000 111",
        "gender", "MALE",
        "currentAddress", "Ha Noi"
    ), 201);
    UUID requestId = UUID.fromString(request.path("id").asText());
    postJson("/api/account-requests", Map.of(
        "organizationCode", "TLKC",
        "displayName", "Pham Thanh Long",
        "email", "long.pt@example.test",
        "phone", "0909 000 111"
    ), 409);

    JsonNode approved = patchJson("/api/organizations/" + clubId + "/account-requests/" + requestId + "/decision", Map.of(
        "status", "APPROVED"
    ), 200);
    assertThat(approved.path("username").asText()).isEqualTo("tlkc.ptlong");
    assertThat(approved.path("temporaryPassword").asText()).isEqualTo("123456");
    assertThat(approved.path("member").path("personName").asText()).isEqualTo("Phạm Thành Long");

    JsonNode auth = postJson("/api/auth/login", Map.of(
        "email", "tlkc.ptlong",
        "password", "123456"
    ), 200);
    assertThat(auth.path("accessToken").asText()).isNotBlank();
    assertThat(auth.path("user").path("username").asText()).isEqualTo("tlkc.ptlong");

    JsonNode direct = postJson("/api/organizations/" + clubId + "/member-accounts", Map.of(
        "displayName", "Pham Thanh Long",
        "email", "long.two@example.test",
        "phone", "0909 000 222",
        "gender", "MALE",
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    UUID directUserId = UUID.fromString(direct.path("member").path("userId").asText());
    assertThat(direct.path("username").asText()).isEqualTo("tlkc.ptlong1");
    assertThat(direct.path("temporaryPassword").asText()).isEqualTo("123456");

    runAs(UUID.randomUUID(), clubId, Set.of(SystemRole.CLUB_MANAGER));
    try {
      postJson("/api/organizations/" + clubId + "/users/" + directUserId + "/club-manager-role", Map.of(), 403);
    } finally {
      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
    }
    JsonNode role = postJson("/api/organizations/" + clubId + "/users/" + directUserId + "/club-manager-role", Map.of(), 200);
    assertThat(role.path("role").asText()).isEqualTo("CLUB_MANAGER");
  }

  @Test
  void rejectRequiresReasonAndStoresRejectedStatus() throws Exception {
    UUID clubId = createClub("Request Reject Club", "RJC");
    UUID requestId = UUID.fromString(postJson("/api/account-requests", Map.of(
        "organizationCode", "RJC",
        "displayName", "Reject Me",
        "email", "reject-me@example.test",
        "phone", "0909 555 000"
    ), 201).path("id").asText());

    patchJson("/api/organizations/" + clubId + "/account-requests/" + requestId + "/decision", Map.of(
        "status", "REJECTED"
    ), 400);

    patchJson("/api/organizations/" + clubId + "/account-requests/" + requestId + "/decision", Map.of(
        "status", "REJECTED",
        "decisionNote", "Thông tin chưa khớp danh sách CLB"
    ), 200);
    JsonNode rejected = getJson("/api/organizations/" + clubId + "/account-requests?status=REJECTED", 200);
    assertThat(rejected).hasSize(1);
    assertThat(rejected.get(0).path("status").asText()).isEqualTo("REJECTED");
    assertThat(rejected.get(0).path("decisionNote").asText()).contains("Thông tin");
  }

  private UUID createClub(String name, String code) throws Exception {
    return UUID.fromString(postJson("/api/organizations", Map.of(
        "name", name,
        "shortName", code,
        "code", code,
        "type", "CLUB"
    ), 201).path("id").asText());
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

  private JsonNode patchJson(String url, Object body, int expectedStatus) throws Exception {
    String content = mvc.perform(patch(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().is(expectedStatus))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private JsonNode getJson(String url, int expectedStatus) throws Exception {
    String content = mvc.perform(get(url))
        .andExpect(status().is(expectedStatus))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private void runAs(UUID userId, UUID organizationId, Set<SystemRole> roles) {
    GlobalAdminCurrentActorProvider.setTestActor(new CurrentActor(userId, organizationId, roles));
    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, organizationId, "club-manager@test.local", "Club Manager", roles);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
  }

  private JsonNode unwrap(String content, int expectedStatus) throws Exception {
    if (content.isBlank()) {
      return objectMapper.createObjectNode();
    }
    JsonNode root = objectMapper.readTree(content);
    assertThat(root.path("success").asBoolean()).isEqualTo(expectedStatus < 400);
    assertThat(root.path("status").asInt()).isEqualTo(expectedStatus);
    assertThat(root.path("code").asText()).isNotBlank();
    return expectedStatus < 400 ? root.path("data") : root;
  }
}
