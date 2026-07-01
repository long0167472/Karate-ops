package com.karate.tournament.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karate.tournament.auth.AuthenticatedPrincipal;
import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.auth.GlobalAdminCurrentActorProvider;
import com.karate.tournament.service.AttendanceLeaveRequestService;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.Person;
import java.time.Instant;
import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.OrganizationType;
import com.karate.tournament.entity.enums.SystemRole;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.OrganizationMemberRepository;
import com.karate.tournament.repository.OrganizationRepository;
import com.karate.tournament.repository.PersonRepository;
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
class ClubOperationsApiTest {
  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  PersonRepository persons;

  @Autowired
  AppUserRepository users;

  @Autowired
  OrganizationMemberRepository members;

  @Autowired
  AttendanceLeaveRequestService attendanceLeaveRequests;

  @Test
  void clubManagerFlowCreatesRosterAttendanceAndDashboardSignals() throws Exception {
    UUID organization = id(postJson("/api/organizations", Map.of("name", "Saigon Karate Club", "type", "CLUB"), 201));
    UUID person = id(postJson("/api/persons", Map.of("displayName", "Le Thi Kumite", "gender", "FEMALE"), 201));
    UUID athlete = id(postJson("/api/athletes", Map.of("personId", person.toString()), 201));

    postJson("/api/organizations/" + organization + "/roster", Map.of("athleteId", athlete.toString()), 409);

    UUID member = id(postJson("/api/organizations/" + organization + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201));
    UUID roster = id(postJson("/api/organizations/" + organization + "/roster", Map.of(
        "athleteId", athlete.toString(),
        "status", "ACTIVE"
    ), 201));
    postJson("/api/organizations/" + organization + "/roster", Map.of("athleteId", athlete.toString()), 409);

    JsonNode rosterRows = getJson("/api/organizations/" + organization + "/roster");
    assertThat(rosterRows.findValuesAsText("id")).contains(roster.toString());

    UUID session = id(postJson("/api/organizations/" + organization + "/attendance-sessions", Map.of(
        "name", "Training 2026-06-09",
        "type", "TRAINING",
        "scheduledAt", "2026-06-09T10:00:00Z"
    ), 201));
    JsonNode record = postJson("/api/attendance-sessions/" + session + "/records", Map.of(
        "athleteId", athlete.toString(),
        "status", "PRESENT"
    ), 201);
    UUID recordId = UUID.fromString(record.path("id").asText());
    assertThat(record.path("organizationMemberId").asText()).isEqualTo(member.toString());
    assertThat(record.path("status").asText()).isEqualTo("PRESENT");

    JsonNode updatedRecord = patchJson("/api/attendance-sessions/" + session + "/records/" + recordId, Map.of(
        "status", "LATE",
        "note", "Traffic"
    ), 200);
    assertThat(updatedRecord.path("status").asText()).isEqualTo("LATE");

    JsonNode sessionDetails = getJson("/api/attendance-sessions/" + session);
    assertThat(sessionDetails.path("records")).hasSize(1);
    assertThat(sessionDetails.path("records").get(0).path("athleteId").asText()).isEqualTo(athlete.toString());

    JsonNode overview = getJson("/api/dashboard/organizations/" + organization + "/overview");
    assertThat(overview.path("activeMembers").asLong()).isEqualTo(1);
    assertThat(overview.path("activeAthletes").asLong()).isEqualTo(1);
    assertThat(overview.path("attendanceSessions").asLong()).isEqualTo(1);
    assertThat(overview.path("attendanceRate").asDouble()).isEqualTo(100.0);

    JsonNode attendance = getJson("/api/dashboard/organizations/" + organization + "/attendance?from=2026-06-01&to=2026-06-30");
    assertThat(attendance.path("sessions").asLong()).isEqualTo(1);
    assertThat(attendance.path("late").asLong()).isEqualTo(1);
    assertThat(attendance.path("lowAttendance")).hasSize(1);

    JsonNode athleteDashboard = getJson("/api/dashboard/organizations/" + organization + "/athletes/" + athlete);
    assertThat(athleteDashboard.path("athleteName").asText()).isEqualTo("Le Thi Kumite");
    assertThat(athleteDashboard.path("sessions").asLong()).isEqualTo(1);
    assertThat(athleteDashboard.path("late").asLong()).isEqualTo(1);
  }

  @Test
  void rejectsCrossClubRosterAttendanceAndTournamentEntryMistakes() throws Exception {
    UUID orgA = id(postJson("/api/organizations", Map.of("name", "Red Club", "type", "CLUB"), 201));
    UUID orgB = id(postJson("/api/organizations", Map.of("name", "Blue Club", "type", "CLUB"), 201));
    UUID personA = id(postJson("/api/persons", Map.of("displayName", "Red Athlete"), 201));
    UUID personB = id(postJson("/api/persons", Map.of("displayName", "Blue Athlete"), 201));
    UUID athleteB = id(postJson("/api/athletes", Map.of(
        "personId", personB.toString(),
        "primaryOrganizationId", orgB.toString()
    ), 201));

    postJson("/api/organizations/" + orgA + "/members", Map.of(
        "personId", personA.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    postJson("/api/organizations/" + orgA + "/roster", Map.of("athleteId", athleteB.toString()), 409);

    UUID wrongMember = id(postJson("/api/organizations/" + orgB + "/members", Map.of(
        "personId", personB.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201));
    UUID sessionA = id(postJson("/api/organizations/" + orgA + "/attendance-sessions", Map.of("name", "Red Training"), 201));
    postJson("/api/attendance-sessions/" + sessionA + "/records", Map.of(
        "organizationMemberId", wrongMember.toString(),
        "status", "PRESENT"
    ), 400);

    UUID tournament = id(postJson("/api/tournaments", Map.of(
        "name", "Roster Protected Cup",
        "ownerOrganizationId", orgA.toString(),
        "visibility", "PUBLIC"
    ), 201));
    UUID participantA = id(postJson("/api/tournaments/" + tournament + "/participants", Map.of(
        "organizationId", orgA.toString(),
        "status", "APPROVED"
    ), 201));
    UUID category = id(postJson("/api/tournaments/" + tournament + "/categories", Map.of(
        "name", "Cadet Kumite",
        "discipline", "KUMITE"
    ), 201));
    postJson("/api/categories/" + category + "/entries", Map.of(
        "tournamentParticipantId", participantA.toString(),
        "athleteId", athleteB.toString()
    ), 409);
  }

  @Test
  void memberCanOnlyUseSelfApisAndRequestLeaveForApproval() throws Exception {
    Organization organization = Organization.create();
    organization.name = "Member Self Club";
    organization.type = OrganizationType.CLUB;
    organizations.save(organization);

    Person person = Person.create();
    person.displayName = "Member One";
    persons.save(person);

    AppUser user = AppUser.create();
    user.displayName = "Member One";
    user.email = "member-one@test.local";
    user.status = "ACTIVE";
    user.primaryOrganization = organization;
    users.save(user);

    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = ClubMemberRole.ATHLETE;
    member.status = ClubMemberStatus.ACTIVE;
    members.save(member);

    UUID session = id(postJson("/api/organizations/" + organization.id + "/attendance-sessions", Map.of(
        "name", "Member Training",
        "scheduledAt", "2026-06-10T11:00:00Z"
    ), 201));

    runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
    try {
      JsonNode profile = getJson("/api/me/club-profile");
      assertThat(profile.path("memberships")).hasSize(1);
      assertThat(profile.path("memberships").get(0).path("id").asText()).isEqualTo(member.id.toString());

      getJson("/api/organizations/" + organization.id + "/members", 403);

      JsonNode leave = postJson("/api/me/attendance/leave-requests", Map.of(
          "requestType", "LEAVE_SESSION",
          "sessionId", session.toString(),
          "reason", "School exam"
      ), 201);
      assertThat(leave.path("status").asText()).isEqualTo("PENDING");
      UUID leaveRequestId = UUID.fromString(leave.path("id").asText());

      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
      JsonNode approved = patchJson("/api/attendance-leave-requests/" + leaveRequestId + "/decision", Map.of(
          "status", "APPROVED",
          "decisionNote", "Approved by admin"
      ), 200);
      assertThat(approved.path("status").asText()).isEqualTo("APPROVED");

      runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
      JsonNode attendance = getJson("/api/me/attendance");
      assertThat(attendance.path("excused").asLong()).isEqualTo(1);
      assertThat(attendance.path("sessionRows").get(0).path("record").path("status").asText()).isEqualTo("EXCUSED");
    } finally {
      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
    }
  }

  @Test
  void managedClubsReturnsOnlyClubsWithAggregatedOverview() throws Exception {
    UUID club = id(postJson("/api/organizations", Map.of("name", "Managed Club", "type", "CLUB"), 201));
    UUID organizer = id(postJson("/api/organizations", Map.of("name", "Not A Club", "type", "ORGANIZER"), 201));
    UUID person = id(postJson("/api/persons", Map.of("displayName", "Managed Athlete"), 201));
    UUID athlete = id(postJson("/api/athletes", Map.of(
        "personId", person.toString(),
        "primaryOrganizationId", club.toString()
    ), 201));
    postJson("/api/organizations/" + club + "/members", Map.of(
        "personId", person.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201);
    postJson("/api/organizations/" + club + "/roster", Map.of(
        "athleteId", athlete.toString(),
        "status", "ACTIVE"
    ), 201);
    postJson("/api/organizations/" + club + "/attendance-sessions", Map.of(
        "name", "Managed Session",
        "scheduledAt", "2026-06-12T11:00:00Z"
    ), 201);

    JsonNode managed = getJson("/api/organizations/managed-clubs");
    assertThat(managed).hasSizeGreaterThanOrEqualTo(1);
    JsonNode clubRow = managedClub(managed, club);
    assertThat(clubRow.path("club").path("id").asText()).isEqualTo(club.toString());
    assertThat(clubRow.path("overview").path("activeMembers").asLong()).isEqualTo(1);
    assertThat(clubRow.path("overview").path("activeAthletes").asLong()).isEqualTo(1);
    assertThat(clubRow.path("overview").path("attendanceSessions").asLong()).isEqualTo(1);
    assertThat(managed.findValuesAsText("id")).doesNotContain(organizer.toString());
  }

  @Test
  void memberAttendanceRespectsDisabledFlag() throws Exception {
    Organization organization = Organization.create();
    organization.name = "Hidden Attendance Club";
    organization.type = OrganizationType.CLUB;
    organizations.save(organization);

    Person person = Person.create();
    person.displayName = "Hidden Member";
    persons.save(person);

    AppUser user = AppUser.create();
    user.displayName = "Hidden Member";
    user.email = "hidden-member@test.local";
    user.status = "ACTIVE";
    user.primaryOrganization = organization;
    users.save(user);

    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = ClubMemberRole.ATHLETE;
    member.status = ClubMemberStatus.ACTIVE;
    member.attendanceViewEnabled = false;
    members.save(member);

    runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
    try {
      JsonNode response = getJson("/api/me/attendance", 403);
      assertThat(response.path("message").asText()).contains("disabled");
    } finally {
      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
    }
  }

  @Test
  void longTermLeaveAppearsInSummaryAndMaterializesForNewSessions() throws Exception {
    Organization organization = Organization.create();
    organization.name = "Long Term Leave Club";
    organization.type = OrganizationType.CLUB;
    organizations.save(organization);

    Person person = Person.create();
    person.displayName = "Long Term Member";
    persons.save(person);

    AppUser user = AppUser.create();
    user.displayName = "Long Term Member";
    user.email = "long-term@test.local";
    user.status = "ACTIVE";
    user.primaryOrganization = organization;
    users.save(user);

    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = ClubMemberRole.ATHLETE;
    member.status = ClubMemberStatus.ACTIVE;
    members.save(member);

    UUID athlete = id(postJson("/api/organizations/" + organization.id + "/athletes", Map.of(
        "personId", person.id.toString(),
        "primaryOrganizationId", organization.id.toString()
    ), 201));
    postJson("/api/organizations/" + organization.id + "/roster", Map.of(
        "athleteId", athlete.toString(),
        "status", "ACTIVE"
    ), 201);

    runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
    try {
      JsonNode leave = postJson("/api/me/attendance/leave-requests", Map.of(
          "requestType", "LEAVE_LONG_TERM",
          "fromDate", "2026-06-20",
          "toDate", "2026-06-22",
          "reason", "Family trip"
      ), 201);
      UUID leaveRequestId = UUID.fromString(leave.path("id").asText());

      JsonNode pendingAttendance = getJson("/api/me/attendance");
      assertThat(pendingAttendance.path("pendingLeaveRequests").asLong()).isEqualTo(1);
      assertThat(pendingAttendance.path("leaveRequests")).hasSize(1);
      assertThat(pendingAttendance.path("leaveRequests").get(0).path("requestType").asText()).isEqualTo("LEAVE_LONG_TERM");

      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
      patchJson("/api/attendance-leave-requests/" + leaveRequestId + "/decision", Map.of(
          "status", "APPROVED",
          "decisionNote", "Approved for trip"
      ), 200);

      UUID session = id(postJson("/api/organizations/" + organization.id + "/attendance-sessions", Map.of(
          "name", "Session During Leave",
          "scheduledAt", "2026-06-21T10:00:00Z"
      ), 201));

      runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
      JsonNode approvedAttendance = getJson("/api/me/attendance");
      assertThat(approvedAttendance.path("pendingLeaveRequests").asLong()).isEqualTo(0);
      assertThat(approvedAttendance.path("excused").asLong()).isEqualTo(1);
      assertThat(approvedAttendance.path("leaveRequests").get(0).path("status").asText()).isEqualTo("APPROVED");
      assertThat(sessionRow(approvedAttendance.path("sessionRows"), session).path("record").path("status").asText()).isEqualTo("EXCUSED");
    } finally {
      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
    }
  }

  @Test
  void lateLeaveApprovalMaterializesLateAttendance() throws Exception {
    Organization organization = Organization.create();
    organization.name = "Late Request Club";
    organization.type = OrganizationType.CLUB;
    organizations.save(organization);

    Person person = Person.create();
    person.displayName = "Late Member";
    persons.save(person);

    AppUser user = AppUser.create();
    user.displayName = "Late Member";
    user.email = "late-member@test.local";
    user.status = "ACTIVE";
    user.primaryOrganization = organization;
    users.save(user);

    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = ClubMemberRole.ATHLETE;
    member.status = ClubMemberStatus.ACTIVE;
    members.save(member);

    UUID athlete = id(postJson("/api/organizations/" + organization.id + "/athletes", Map.of(
        "personId", person.id.toString(),
        "primaryOrganizationId", organization.id.toString()
    ), 201));
    postJson("/api/organizations/" + organization.id + "/roster", Map.of(
        "athleteId", athlete.toString(),
        "status", "ACTIVE"
    ), 201);
    UUID session = id(postJson("/api/organizations/" + organization.id + "/attendance-sessions", Map.of(
        "name", "Late Session",
        "scheduledAt", "2026-06-16T11:00:00Z"
    ), 201));

    runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
    try {
      JsonNode leave = postJson("/api/me/attendance/leave-requests", Map.of(
          "requestType", "LATE",
          "sessionId", session.toString(),
          "reason", "Traffic jam"
      ), 201);
      UUID leaveRequestId = UUID.fromString(leave.path("id").asText());

      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
      patchJson("/api/attendance-leave-requests/" + leaveRequestId + "/decision", Map.of(
          "status", "APPROVED",
          "decisionNote", "Late accepted"
      ), 200);

      runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
      JsonNode attendance = getJson("/api/me/attendance");
      assertThat(attendance.path("late").asLong()).isEqualTo(1);
      assertThat(sessionRow(attendance.path("sessionRows"), session).path("record").path("status").asText()).isEqualTo("LATE");
    } finally {
      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
    }
  }

  @Test
  void expiredLeaveRequestsTurnIntoAutoAbsent() throws Exception {
    Organization organization = Organization.create();
    organization.name = "Expiry Club";
    organization.type = OrganizationType.CLUB;
    organizations.save(organization);

    Person person = Person.create();
    person.displayName = "Expiry Member";
    persons.save(person);

    AppUser user = AppUser.create();
    user.displayName = "Expiry Member";
    user.email = "expiry-member@test.local";
    user.status = "ACTIVE";
    user.primaryOrganization = organization;
    users.save(user);

    OrganizationMember member = OrganizationMember.create();
    member.organization = organization;
    member.person = person;
    member.user = user;
    member.role = ClubMemberRole.ATHLETE;
    member.status = ClubMemberStatus.ACTIVE;
    members.save(member);

    UUID athlete = id(postJson("/api/organizations/" + organization.id + "/athletes", Map.of(
        "personId", person.id.toString(),
        "primaryOrganizationId", organization.id.toString()
    ), 201));
    postJson("/api/organizations/" + organization.id + "/roster", Map.of(
        "athleteId", athlete.toString(),
        "status", "ACTIVE"
    ), 201);
    UUID session = id(postJson("/api/organizations/" + organization.id + "/attendance-sessions", Map.of(
        "name", "Past Session",
        "scheduledAt", Instant.now().minusSeconds(7200).toString()
    ), 201));

    runAs(user.id, organization.id, Set.of(SystemRole.MEMBER));
    try {
      JsonNode leave = postJson("/api/me/attendance/leave-requests", Map.of(
          "requestType", "LEAVE_SESSION",
          "sessionId", session.toString(),
          "reason", "Missed the alarm"
      ), 201);
      assertThat(leave.path("status").asText()).isEqualTo("PENDING");

      assertThat(attendanceLeaveRequests.expirePendingRequests()).isEqualTo(1);

      JsonNode attendance = getJson("/api/me/attendance");
      assertThat(attendance.path("absent").asLong()).isEqualTo(1);
      assertThat(sessionRow(attendance.path("sessionRows"), session).path("leaveRequest").path("status").asText()).isEqualTo("EXPIRED_AUTO_ABSENT");
      assertThat(sessionRow(attendance.path("sessionRows"), session).path("record").path("status").asText()).isEqualTo("ABSENT");
    } finally {
      SecurityContextHolder.clearContext();
      GlobalAdminCurrentActorProvider.clearTestActor();
    }
  }

  @Test
  void feeGroupsCanBeBulkAssignedAndUsedWhenApplyingFeeItems() throws Exception {
    UUID organization = id(postJson("/api/organizations", Map.of("name", "Fee Group Club", "type", "CLUB"), 201));
    UUID personOne = id(postJson("/api/persons", Map.of("displayName", "Nguyen Fee One"), 201));
    UUID personTwo = id(postJson("/api/persons", Map.of("displayName", "Tran Fee Two"), 201));
    UUID memberOne = id(postJson("/api/organizations/" + organization + "/members", Map.of(
        "personId", personOne.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201));
    UUID memberTwo = id(postJson("/api/organizations/" + organization + "/members", Map.of(
        "personId", personTwo.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201));

    UUID regularRole = id(postJson("/api/organizations/" + organization + "/fee-roles", Map.of(
        "code", "REGULAR",
        "name", "Hoc vien thuong",
        "priority", 100,
        "active", true
    ), 201));
    UUID studentRole = id(postJson("/api/organizations/" + organization + "/fee-roles", Map.of(
        "code", "STUDENT",
        "name", "Sinh vien",
        "priority", 50,
        "active", true
    ), 201));

    JsonNode replaced = putJson("/api/organizations/" + organization + "/members/fee-roles/bulk", Map.of(
        "memberIds", List.of(memberOne.toString(), memberTwo.toString()),
        "feeRoleIds", List.of(regularRole.toString()),
        "mode", "REPLACE"
    ), 200);
    assertThat(replaced).hasSize(2);
    assertThat(replaced.get(0).path("roles")).hasSize(1);

    JsonNode added = putJson("/api/organizations/" + organization + "/members/fee-roles/bulk", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "feeRoleIds", List.of(studentRole.toString()),
        "mode", "ADD"
    ), 200);
    assertThat(added.get(0).path("roles")).hasSize(2);

    JsonNode removed = putJson("/api/organizations/" + organization + "/members/fee-roles/bulk", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "feeRoleIds", List.of(regularRole.toString()),
        "mode", "REMOVE"
    ), 200);
    assertThat(removed.get(0).path("roles")).hasSize(1);
    assertThat(removed.get(0).path("roles").get(0).path("id").asText()).isEqualTo(studentRole.toString());

    putJson("/api/organizations/" + organization + "/members/fee-roles/bulk", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "feeRoleIds", List.of(regularRole.toString()),
        "mode", "ADD"
    ), 200);

    UUID feeItem = id(postJson("/api/organizations/" + organization + "/fee-items", Map.of(
        "name", "Hoc phi thang 6",
        "feeType", "TUITION",
        "billingCycle", "MONTHLY",
        "status", "ACTIVE",
        "defaultAmount", 400000,
        "dueDay", 10,
        "roleAmounts", List.of(
            Map.of("feeRoleId", regularRole.toString(), "amount", 300000, "exempt", false),
            Map.of("feeRoleId", studentRole.toString(), "amount", 200000, "exempt", false)
        )
    ), 201));

    JsonNode assignments = postJson("/api/organizations/" + organization + "/fee-items/" + feeItem + "/apply", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "feeRoleIds", List.of(regularRole.toString()),
        "dueDate", "2026-06-10",
        "note", "Apply by explicit member and fee group"
    ), 200);
    assertThat(assignments).hasSize(2);

    JsonNode memberOneAssignment = assignmentForMember(assignments, memberOne);
    JsonNode memberTwoAssignment = assignmentForMember(assignments, memberTwo);
    assertThat(memberOneAssignment.path("amountDue").asLong()).isEqualTo(200000);
    assertThat(memberOneAssignment.path("assignedRoleName").asText()).isEqualTo("Sinh vien");
    assertThat(memberTwoAssignment.path("amountDue").asLong()).isEqualTo(300000);
    assertThat(memberTwoAssignment.path("assignedRoleName").asText()).isEqualTo("Hoc vien thuong");
  }

  @Test
  void financeFlowUsesDefaultTuitionOverridesOneTimeIncomeExpensesAndDashboard() throws Exception {
    UUID organization = id(postJson("/api/organizations", Map.of("name", "Finance Club", "type", "CLUB"), 201));
    UUID personOne = id(postJson("/api/persons", Map.of("displayName", "Pham Finance One"), 201));
    UUID personTwo = id(postJson("/api/persons", Map.of("displayName", "Do Finance Two"), 201));
    UUID memberOne = id(postJson("/api/organizations/" + organization + "/members", Map.of(
        "personId", personOne.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201));
    UUID memberTwo = id(postJson("/api/organizations/" + organization + "/members", Map.of(
        "personId", personTwo.toString(),
        "role", "ATHLETE",
        "status", "ACTIVE"
    ), 201));

    JsonNode createdOverview = getJson("/api/organizations/" + organization + "/finance/overview");
    JsonNode defaultTuition = feeItemByKind(createdOverview.path("feeItems"), "MONTHLY_TUITION_DEFAULT");
    assertThat(defaultTuition.path("name").asText()).isEqualTo("Học phí");
    UUID defaultTuitionId = UUID.fromString(defaultTuition.path("id").asText());

    patchJson("/api/organizations/" + organization + "/fee-items/" + defaultTuitionId, Map.of(
        "name", "Học phí",
        "feeType", "TUITION",
        "feeKind", "MONTHLY_TUITION_DEFAULT",
        "billingCycle", "MONTHLY",
        "status", "ACTIVE",
        "defaultAmount", 250000,
        "dueDay", 10
    ), 200);

    UUID studentTuition = id(postJson("/api/organizations/" + organization + "/fee-items", Map.of(
        "name", "Sinh viên",
        "feeType", "TUITION",
        "feeKind", "MONTHLY_TUITION_OVERRIDE",
        "billingCycle", "MONTHLY",
        "status", "ACTIVE",
        "defaultAmount", 150000,
        "dueDay", 10
    ), 201));

    JsonNode assignedOverride = putJson("/api/organizations/" + organization + "/finance/tuition-overrides/bulk", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "feeItemId", studentTuition.toString()
    ), 200);
    assertThat(assignedOverride).hasSize(1);
    assertThat(assignedOverride.get(0).path("feeItemName").asText()).isEqualTo("Sinh viên");

    JsonNode overrideOverview = getJson("/api/organizations/" + organization + "/finance/overview");
    assertThat(overrideOverview.path("summary").path("monthlyTuitionExpected").asLong()).isEqualTo(400000);

    putJson("/api/organizations/" + organization + "/finance/tuition-overrides/bulk", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "feeItemId", defaultTuitionId.toString()
    ), 200);
    JsonNode clearedOverview = getJson("/api/organizations/" + organization + "/finance/overview");
    assertThat(clearedOverview.path("summary").path("monthlyTuitionExpected").asLong()).isEqualTo(500000);

    UUID uniformFee = id(postJson("/api/organizations/" + organization + "/fee-items", Map.of(
        "name", "Đồng phục",
        "feeType", "UNIFORM",
        "feeKind", "ONE_TIME_INCOME",
        "billingCycle", "ONE_TIME",
        "status", "ACTIVE",
        "defaultAmount", 450000
    ), 201));

    JsonNode firstAssignments = postJson("/api/organizations/" + organization + "/fee-items/" + uniformFee + "/apply", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "dueDate", "2026-06-30",
        "note", "Áp dụng đồng phục"
    ), 200);
    JsonNode duplicateAssignments = postJson("/api/organizations/" + organization + "/fee-items/" + uniformFee + "/apply", Map.of(
        "memberIds", List.of(memberOne.toString()),
        "dueDate", "2026-06-30"
    ), 200);
    assertThat(firstAssignments).hasSize(1);
    assertThat(duplicateAssignments).hasSize(1);
    UUID assignmentId = UUID.fromString(firstAssignments.get(0).path("id").asText());
    assertThat(firstAssignments.get(0).path("amountDue").asLong()).isEqualTo(450000);

    patchJson("/api/organizations/" + organization + "/fee-assignments/" + assignmentId, Map.of(
        "paidAmount", 450000,
        "status", "PAID"
    ), 200);

    UUID expense = id(postJson("/api/organizations/" + organization + "/finance/expenses", Map.of(
        "name", "Thuê sân",
        "amount", 120000,
        "expenseDate", "2026-06-10",
        "status", "PENDING_DISBURSEMENT",
        "note", "Ca tối"
    ), 201));
    patchJson("/api/organizations/" + organization + "/finance/expenses/" + expense, Map.of(
        "name", "Thuê sân",
        "amount", 120000,
        "expenseDate", "2026-06-10",
        "status", "DISBURSED",
        "note", "Đã chuyển khoản"
    ), 200);

    JsonNode financeOverview = getJson("/api/organizations/" + organization + "/finance/overview");
    assertThat(financeOverview.path("summary").path("oneTimeIncomeDue").asLong()).isEqualTo(450000);
    assertThat(financeOverview.path("summary").path("totalPaid").asLong()).isEqualTo(450000);
    assertThat(financeOverview.path("summary").path("expensesDisbursed").asLong()).isEqualTo(120000);
    assertThat(financeOverview.path("summary").path("netCash").asLong()).isEqualTo(330000);
    assertThat(financeOverview.path("expenses")).hasSize(1);

    JsonNode listedExpenses = getJson("/api/organizations/" + organization + "/finance/expenses");
    assertThat(listedExpenses).hasSize(1);

    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/organizations/" + organization + "/finance/expenses/" + expense))
        .andExpect(status().isNoContent());
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

  private JsonNode putJson(String url, Object body, int expectedStatus) throws Exception {
    String content = mvc.perform(put(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().is(expectedStatus))
        .andReturn()
        .getResponse()
        .getContentAsString();
    return unwrap(content, expectedStatus);
  }

  private JsonNode getJson(String url) throws Exception {
    return getJson(url, 200);
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
    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, organizationId, "member@test.local", "Member", roles);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
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
    assertThat(root.path("code").asText()).isNotBlank();
    return expectedStatus < 400 ? root.path("data") : root;
  }

  private JsonNode assignmentForMember(JsonNode assignments, UUID memberId) {
    for (JsonNode assignment : assignments) {
      if (assignment.path("memberId").asText().equals(memberId.toString())) {
        return assignment;
      }
    }
    throw new AssertionError("Missing assignment for member " + memberId);
  }

  private JsonNode feeItemByKind(JsonNode feeItems, String kind) {
    for (JsonNode item : feeItems) {
      if (item.path("feeKind").asText().equals(kind)) {
        return item;
      }
    }
    throw new AssertionError("Missing fee item kind " + kind);
  }

  private JsonNode managedClub(JsonNode clubs, UUID clubId) {
    for (JsonNode club : clubs) {
      if (club.path("club").path("id").asText().equals(clubId.toString())) {
        return club;
      }
    }
    throw new AssertionError("Missing managed club " + clubId);
  }

  private JsonNode sessionRow(JsonNode rows, UUID sessionId) {
    for (JsonNode row : rows) {
      if (row.path("id").asText().equals(sessionId.toString())) {
        return row;
      }
    }
    throw new AssertionError("Missing session row " + sessionId);
  }
}
