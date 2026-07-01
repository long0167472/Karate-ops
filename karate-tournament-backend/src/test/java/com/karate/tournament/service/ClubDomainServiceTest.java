package com.karate.tournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.karate.tournament.entity.enums.AttendanceRecordStatus;
import com.karate.tournament.entity.enums.AttendanceSessionStatus;
import com.karate.tournament.entity.enums.AttendanceSessionType;
import com.karate.tournament.entity.enums.ClubMemberRole;
import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.enums.ClubRosterStatus;
import com.karate.tournament.entity.enums.OrganizationType;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.dto.request.AthleteCreateRequest;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.dto.request.AttendanceRecordRequest;
import com.karate.tournament.dto.response.AttendanceRecordResponse;
import com.karate.tournament.dto.request.AttendanceRecordUpdateRequest;
import com.karate.tournament.dto.request.AttendanceSessionCreateRequest;
import com.karate.tournament.dto.response.AttendanceSessionResponse;
import com.karate.tournament.dto.request.ClubMemberCreateRequest;
import com.karate.tournament.dto.response.ClubMemberResponse;
import com.karate.tournament.dto.request.ClubRosterCreateRequest;
import com.karate.tournament.dto.request.OrganizationCreateRequest;
import com.karate.tournament.dto.response.OrganizationResponse;
import com.karate.tournament.dto.request.PersonCreateRequest;
import com.karate.tournament.dto.response.PersonResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ClubDomainServiceTest {
  @Autowired
  OrganizationService organizations;

  @Autowired
  PersonService persons;

  @Autowired
  AthleteService athletes;

  @Autowired
  OrganizationMemberService members;

  @Autowired
  ClubRosterService roster;

  @Autowired
  AttendanceService attendance;

  @Test
  void serviceRulesRequireMembershipBeforeRosterAndKeepAttendanceHistoryOnSoftDelete() {
    OrganizationResponse organization = organizations.create(new OrganizationCreateRequest(
        "Service Test Club", null, null, OrganizationType.CLUB, null, null, null, null, null
    ));
    PersonResponse person = persons.create(new PersonCreateRequest(
        "Service Athlete", null, null, null, null, null, null, null, null, null, null
    ));
    AthleteResponse athlete = athletes.create(new AthleteCreateRequest(
        person.id(), null, null, null, null, null, null
    ));

    assertThatThrownBy(() -> roster.create(organization.id(), new ClubRosterCreateRequest(
        athlete.id(), ClubRosterStatus.ACTIVE, null
    ))).isInstanceOf(BusinessConflictException.class)
        .hasMessageContaining("ACTIVE club member");

    ClubMemberResponse member = members.create(organization.id(), new ClubMemberCreateRequest(
        person.id(), null, ClubMemberRole.ATHLETE, ClubMemberStatus.ACTIVE, null, null, null, null, null
    ));
    roster.create(organization.id(), new ClubRosterCreateRequest(athlete.id(), ClubRosterStatus.ACTIVE, null));

    assertThatThrownBy(() -> roster.create(organization.id(), new ClubRosterCreateRequest(
        athlete.id(), ClubRosterStatus.ACTIVE, null
    ))).isInstanceOf(BusinessConflictException.class)
        .hasMessageContaining("already in this club roster");

    AttendanceSessionResponse session = attendance.create(organization.id(), new AttendanceSessionCreateRequest(
        "Service Training",
        AttendanceSessionType.TRAINING,
        AttendanceSessionStatus.OPEN,
        Instant.parse("2026-06-09T10:00:00Z"),
        null,
        null
    ));
    AttendanceRecordResponse record = attendance.record(session.id(), new AttendanceRecordRequest(
        null,
        athlete.id(),
        AttendanceRecordStatus.ABSENT,
        null,
        "Competition rest"
    ));
    assertThat(record.organizationMemberId()).isEqualTo(member.id());
    assertThat(record.status()).isEqualTo(AttendanceRecordStatus.ABSENT);

    AttendanceRecordResponse updated = attendance.updateRecord(session.id(), record.id(), new AttendanceRecordUpdateRequest(
        AttendanceRecordStatus.EXCUSED,
        null,
        "Medical note"
    ));
    assertThat(updated.status()).isEqualTo(AttendanceRecordStatus.EXCUSED);

    members.delete(organization.id(), member.id());
    assertThat(members.list(organization.id()))
        .extracting(ClubMemberResponse::id)
        .doesNotContain(member.id());
    assertThat(attendance.get(session.id()).records()).hasSize(1);
  }
}
