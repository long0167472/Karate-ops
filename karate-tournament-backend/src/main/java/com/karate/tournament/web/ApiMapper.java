package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.entity.Athlete;
import com.karate.tournament.entity.AttendanceRecord;
import com.karate.tournament.entity.AttendanceLeaveRequest;
import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.AccountRequest;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.ClubRoster;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.KataVote;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.MatchScoreEvent;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.entity.OrganizationMember;
import com.karate.tournament.entity.Person;
import com.karate.tournament.entity.Tatami;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.repository.AttendanceRecordRepository;
import com.karate.tournament.repository.KataVoteRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchScoreEventRepository;
import com.karate.tournament.dto.response.AttendanceRecordResponse;
import com.karate.tournament.dto.response.AttendanceSessionResponse;
import com.karate.tournament.dto.response.AccountRequestResponse;
import com.karate.tournament.dto.response.AthleteResponse;
import com.karate.tournament.dto.response.CategoryResponse;
import com.karate.tournament.dto.response.ClubMemberResponse;
import com.karate.tournament.dto.response.ClubRosterResponse;
import com.karate.tournament.dto.response.EntryResponse;
import com.karate.tournament.dto.response.KataVoteResponse;
import com.karate.tournament.dto.response.KumiteStateResponse;
import com.karate.tournament.dto.response.LeaveRequestResponse;
import com.karate.tournament.dto.response.MatchEventResponse;
import com.karate.tournament.dto.response.MatchParticipantResponse;
import com.karate.tournament.dto.response.MatchResponse;
import com.karate.tournament.dto.response.OrganizationResponse;
import com.karate.tournament.dto.response.PersonResponse;
import com.karate.tournament.dto.response.PublicClubLookupResponse;
import com.karate.tournament.dto.response.TatamiResponse;
import com.karate.tournament.dto.response.TournamentParticipantResponse;
import com.karate.tournament.dto.response.TournamentResponse;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiMapper {
  private final MatchParticipantRepository matchParticipants;
  private final KumiteMatchStateRepository kumiteStates;
  private final KataVoteRepository kataVotes;
  private final MatchScoreEventRepository scoreEvents;
  private final AttendanceRecordRepository attendanceRecords;

  public OrganizationResponse organization(Organization organization) {
    return new OrganizationResponse(
        organization.id,
        organization.name,
        organization.shortName,
        organization.code,
        organization.type,
        organization.status,
        organization.country,
        organization.province,
        organization.address,
        organization.contactEmail,
        organization.contactPhone
    );
  }

  public PublicClubLookupResponse publicClub(Organization organization) {
    return new PublicClubLookupResponse(
        organization.id,
        organization.name,
        organization.shortName,
        organization.code,
        organization.province,
        organization.address,
        organization.contactEmail,
        organization.contactPhone
    );
  }

  public AccountRequestResponse accountRequest(AccountRequest request) {
    return new AccountRequestResponse(
        request.id,
        request.organization.id,
        request.organization.name,
        request.organization.code,
        request.displayName,
        request.email,
        request.phone,
        request.gender,
        request.birthDate,
        request.currentAddress,
        request.status,
        request.decisionNote,
        request.decidedByUser == null ? null : request.decidedByUser.id,
        request.approvedUser == null ? null : request.approvedUser.id,
        request.decidedAt,
        request.createdAt
    );
  }

  public ClubMemberResponse clubMember(OrganizationMember member) {
    return new ClubMemberResponse(
        member.id,
        member.organization.id,
        member.organization.name,
        member.person == null ? null : member.person.id,
        member.person == null ? null : member.person.displayName,
        member.user == null ? null : member.user.id,
        member.user == null ? null : member.user.displayName,
        member.role,
        member.status,
        member.joinedAt,
        member.person == null ? null : member.person.gender,
        member.person == null ? null : member.person.phone,
        member.person == null ? null : member.person.email,
        member.person == null ? null : member.person.currentAddress,
        member.student,
        member.attendanceViewEnabled,
        member.tuitionStatus,
        member.tuitionPaidAmount,
        member.otherFeeStatus,
        member.otherFeePaidAmount,
        member.paymentNote,
        member.memberNote
    );
  }

  public ClubRosterResponse clubRoster(ClubRoster roster) {
    return new ClubRosterResponse(
        roster.id,
        roster.organization.id,
        roster.organization.name,
        roster.athlete.id,
        roster.athlete.person.displayName,
        roster.athlete.person.id,
        roster.status,
        roster.joinedAt
    );
  }

  public AttendanceSessionResponse attendanceSession(AttendanceSession session) {
    List<AttendanceRecordResponse> records = attendanceRecords
        .findBySession_IdAndDeletedAtIsNullOrderByCreatedAtAsc(session.id)
        .stream()
        .map(this::attendanceRecord)
        .toList();
    return attendanceSession(session, records);
  }

  public AttendanceSessionResponse attendanceSession(AttendanceSession session, List<AttendanceRecordResponse> records) {
    return new AttendanceSessionResponse(
        session.id,
        session.organization.id,
        session.organization.name,
        session.tournamentParticipant == null ? null : session.tournamentParticipant.id,
        session.name,
        session.type,
        session.status,
        session.scheduledAt,
        session.source == null ? null : session.source.name(),
        session.scheduledDate,
        session.trainingSchedule == null ? null : session.trainingSchedule.id,
        session.notes,
        records
    );
  }

  public AttendanceRecordResponse attendanceRecord(AttendanceRecord record) {
    Athlete athlete = record.athlete;
    OrganizationMember member = record.organizationMember;
    Person person = athlete != null ? athlete.person : member == null ? null : member.person;
    String displayName = athlete != null
        ? athlete.person.displayName
        : person == null ? null : person.displayName;
    return new AttendanceRecordResponse(
        record.id,
        record.session.id,
        member == null ? null : member.id,
        athlete == null ? null : athlete.id,
        person == null ? null : person.id,
        displayName,
        record.status,
        record.checkInAt,
        record.note
    );
  }

  public LeaveRequestResponse leaveRequest(AttendanceLeaveRequest request) {
    Person person = request.member.person;
    return new LeaveRequestResponse(
        request.id,
        request.session.id,
        request.session.name,
        request.session.organization.id,
        request.session.organization.name,
        request.member.id,
        person == null ? request.member.user == null ? null : request.member.user.displayName : person.displayName,
        request.requesterUser == null ? null : request.requesterUser.id,
        request.decidedByUser == null ? null : request.decidedByUser.id,
        request.status,
        request.reason,
        request.decisionNote,
        request.decidedAt,
        request.createdAt
    );
  }

  public TournamentResponse tournament(Tournament tournament) {
    return new TournamentResponse(
        tournament.id,
        tournament.name,
        tournament.code,
        tournament.description,
        tournament.location,
        tournament.startsOn,
        tournament.endsOn,
        tournament.visibility,
        tournament.status,
        tournament.rulesetVersion,
        tournament.organizerName,
        tournament.tatamiCount,
        splitCsv(tournament.competitionLevels),
        tournament.rulesetPreset,
        tournament.ruleSnapshotJson,
        tournament.ownerOrganization == null ? null : tournament.ownerOrganization.id,
        tournament.ownerOrganization == null ? null : tournament.ownerOrganization.name,
        tournament.createdByUser == null ? null : tournament.createdByUser.id,
        tournament.step,
        tournament.phongTraoEnabled,
        tournament.nangCaoEnabled,
        tournament.registrationDeadline,
        tournament.registrationFee
    );
  }

  public TournamentParticipantResponse participant(TournamentParticipant participant) {
    return new TournamentParticipantResponse(
        participant.id,
        participant.tournament.id,
        participant.organization.id,
        participant.organization.name,
        participant.displayName,
        participant.status,
        participant.approvedAt
    );
  }

  public PersonResponse person(Person person) {
    return new PersonResponse(
        person.id,
        person.displayName,
        person.firstName,
        person.lastName,
        person.birthDate,
        person.gender,
        person.nationalId,
        person.email,
        person.phone,
        person.currentAddress,
        person.emergencyContactName,
        person.emergencyContactPhone
    );
  }

  public AthleteResponse athlete(Athlete athlete) {
    return new AthleteResponse(
        athlete.id,
        athlete.person.id,
        athlete.person.displayName,
        athlete.primaryOrganization == null ? null : athlete.primaryOrganization.id,
        athlete.primaryOrganization == null ? null : athlete.primaryOrganization.name,
        athlete.externalCode,
        athlete.belt,
        athlete.weightKg,
        athlete.heightCm,
        athlete.status
    );
  }

  public CategoryResponse category(Category category) {
    return new CategoryResponse(
        category.id,
        category.tournament.id,
        category.name,
        category.discipline,
        category.gender,
        category.ageMin,
        category.ageMax,
        category.weightMinKg,
        category.weightMaxKg,
        category.competitionLevel,
        category.weightLabel,
        category.openWeight,
        category.entryType,
        category.status,
        category.rulesetVersion,
        category.repechageEnabled,
        category.matchDurationSeconds,
        category.kataJudgeCount,
        category.kataRepeatAllowed,
        category.entryLimitPerOrganization
    );
  }

  public EntryResponse entry(Entry entry) {
    Athlete athlete = entry.athlete;
    return new EntryResponse(
        entry.id,
        entry.category.id,
        entry.tournamentParticipant.id,
        entry.tournamentParticipant.displayName,
        athlete == null ? null : athlete.id,
        athlete == null ? null : athlete.person.displayName,
        entry.teamId,
        entry.seedNo,
        entry.status,
        entry.registrationWeightKg,
        entry.weighInStatus,
        entry.teamName,
        splitUuidCsv(entry.teamMemberAthleteIds),
        entry.validationNotes
    );
  }

  private List<String> splitCsv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .toList();
  }

  private List<UUID> splitUuidCsv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .map(UUID::fromString)
        .toList();
  }

  public TatamiResponse tatami(Tatami tatami) {
    return new TatamiResponse(
        tatami.id,
        tatami.tournament.id,
        tatami.tatamiNo,
        tatami.name,
        tatami.status,
        tatami.currentMatch == null ? null : tatami.currentMatch.id
    );
  }

  public MatchResponse match(Match match) {
    List<MatchParticipantResponse> participantResponses = matchParticipants
        .findByMatch_IdAndDeletedAtIsNullOrderBySideAsc(match.id)
        .stream()
        .map(this::matchParticipant)
        .toList();
    KumiteStateResponse kumite = kumiteStates.findById(match.id).map(this::kumite).orElse(null);
    List<KataVoteResponse> voteResponses = kataVotes.findByMatch_IdAndDeletedAtIsNullOrderByJudgeNumberAsc(match.id)
        .stream()
        .map(this::kataVote)
        .toList();
    List<MatchEventResponse> recentEvents = scoreEvents.findTop80ByMatch_IdAndDeletedAtIsNullOrderByOccurredAtDesc(match.id)
        .stream()
        .sorted(Comparator.comparing(event -> event.occurredAt))
        .map(this::matchEvent)
        .toList();
    return new MatchResponse(
        match.id,
        match.tournament.id,
        match.category.id,
        match.category.name,
        match.tatami == null ? null : match.tatami.id,
        match.tatami == null ? null : match.tatami.tatamiNo,
        match.matchNumber,
        match.roundName,
        match.roundNumber,
        match.bracketPosition,
        match.status,
        match.scheduledAt,
        match.mode,
        match.winnerEntry == null ? null : match.winnerEntry.id,
        match.winnerAthlete == null ? null : match.winnerAthlete.id,
        match.winType,
        participantResponses,
        kumite,
        voteResponses,
        recentEvents
    );
  }

  private MatchParticipantResponse matchParticipant(MatchParticipant participant) {
    Entry entry = participant.entry;
    Athlete athlete = entry == null ? null : entry.athlete;
    return new MatchParticipantResponse(
        entry == null ? null : entry.id,
        athlete == null ? null : athlete.id,
        athlete == null ? null : athlete.person.displayName,
        entry == null ? null : entry.teamId,
        entry == null ? null : entry.tournamentParticipant.displayName,
        participant.side
    );
  }

  private KumiteStateResponse kumite(KumiteMatchState state) {
    return new KumiteStateResponse(
        state.akaScore,
        state.aoScore,
        state.akaSenshu,
        state.aoSenshu,
        state.akaChui,
        state.aoChui,
        state.akaHansokuChui,
        state.aoHansokuChui,
        state.akaHansoku,
        state.aoHansoku,
        state.akaShikkaku,
        state.aoShikkaku,
        state.akaKiken,
        state.aoKiken,
        state.durationMs,
        state.remainingMs,
        state.timerRunning,
        state.timerStartedAt
    );
  }

  private KataVoteResponse kataVote(KataVote vote) {
    return new KataVoteResponse(vote.judgeNumber, vote.side, vote.voteValue);
  }

  private MatchEventResponse matchEvent(MatchScoreEvent event) {
    return new MatchEventResponse(
        event.id,
        event.type,
        event.side,
        event.points,
        event.penaltyCode,
        event.judgeNumber,
        event.voteSide,
        event.occurredAt
    );
  }
}
