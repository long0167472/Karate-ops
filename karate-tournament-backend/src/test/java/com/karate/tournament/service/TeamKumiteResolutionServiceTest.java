package com.karate.tournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamKumiteResolutionServiceTest {
  private MatchRepository matches;
  private MatchParticipantRepository participants;
  private KumiteMatchStateRepository kumiteStates;
  private TeamKumiteResolutionService service;
  private UUID groupId;
  private Entry akaTeam;
  private Entry aoTeam;
  private List<Match> groupMatches;
  private List<MatchParticipant> savedParticipants;
  private List<KumiteMatchState> savedStates;

  @BeforeEach
  void setUp() {
    matches = mock(MatchRepository.class);
    participants = mock(MatchParticipantRepository.class);
    kumiteStates = mock(KumiteMatchStateRepository.class);
    service = new TeamKumiteResolutionService(matches, participants, kumiteStates);
    groupId = UUID.randomUUID();
    akaTeam = entry();
    aoTeam = entry();
    groupMatches = new ArrayList<>();
    savedParticipants = new ArrayList<>();
    savedStates = new ArrayList<>();

    when(matches.findByTeamMatchGroupIdAndDeletedAtIsNullOrderByTeamBoutOrderAscMatchNumberAsc(groupId))
        .thenAnswer(invocation -> List.copyOf(groupMatches));
    when(matches.save(any(Match.class))).thenAnswer(invocation -> {
      Match match = invocation.getArgument(0, Match.class);
      match.id = UUID.randomUUID();
      groupMatches.add(match);
      return match;
    });
    when(participants.save(any(MatchParticipant.class))).thenAnswer(invocation -> {
      MatchParticipant participant = invocation.getArgument(0, MatchParticipant.class);
      participant.id = UUID.randomUUID();
      savedParticipants.add(participant);
      return participant;
    });
    when(kumiteStates.save(any(KumiteMatchState.class))).thenAnswer(invocation -> {
      KumiteMatchState state = invocation.getArgument(0, KumiteMatchState.class);
      state.matchId = state.match.id;
      savedStates.add(state);
      return state;
    });
  }

  @Test
  void resolvesTeamWinnerByBoutVictories() {
    Match bout1 = regularBout(1, Side.AKA, 4, 1);
    regularBout(2, Side.AO, 0, 2);
    regularBout(3, Side.AKA, 3, 1);

    TeamKumiteAggregate aggregate = service.resolveAfterLockedBout(bout1);

    assertThat(aggregate.resolved()).isTrue();
    assertThat(aggregate.winnerSide()).isEqualTo(Side.AKA);
    assertThat(aggregate.reasonCode()).isEqualTo("TEAM_BOUT_VICTORIES");
    assertThat(aggregate.akaBoutVictories()).isEqualTo(2);
    assertThat(aggregate.aoBoutVictories()).isEqualTo(1);
    assertThat(aggregate.extraBoutRequired()).isFalse();
  }

  @Test
  void resolvesTeamWinnerByTotalPointsWhenBoutVictoriesAreTied() {
    Match bout1 = regularBout(1, Side.AKA, 5, 1);
    regularBout(2, Side.AO, 0, 3);

    TeamKumiteAggregate aggregate = service.resolveAfterLockedBout(bout1);

    assertThat(aggregate.resolved()).isTrue();
    assertThat(aggregate.winnerSide()).isEqualTo(Side.AKA);
    assertThat(aggregate.reasonCode()).isEqualTo("TEAM_TOTAL_POINTS");
    assertThat(aggregate.akaTotalPoints()).isEqualTo(5);
    assertThat(aggregate.aoTotalPoints()).isEqualTo(4);
  }

  @Test
  void createsExtraBoutWhenBoutVictoriesAndTotalPointsAreStillTied() {
    Match bout1 = regularBout(1, Side.AKA, 3, 2);
    regularBout(2, Side.AO, 2, 3);

    TeamKumiteAggregate aggregate = service.resolveAfterLockedBout(bout1);

    assertThat(aggregate.resolved()).isFalse();
    assertThat(aggregate.extraBoutRequired()).isTrue();
    assertThat(aggregate.createdExtraBout()).isNotNull();
    assertThat(aggregate.createdExtraBout().teamExtraBout).isTrue();
    assertThat(aggregate.createdExtraBout().teamMatchGroupId).isEqualTo(groupId);
    assertThat(aggregate.createdExtraBout().roundName).contains("Extra Bout");
    assertThat(savedParticipants)
        .extracting(participant -> participant.side)
        .containsExactly(Side.AKA, Side.AO);
    assertThat(savedStates).hasSize(1);
  }

  @Test
  void resolvesTeamWinnerFromExtraBoutIncludingHantei() {
    Match bout1 = regularBout(1, Side.AKA, 3, 2);
    regularBout(2, Side.AO, 2, 3);
    extraBout(3, Side.AO, WinType.HANTEI);

    TeamKumiteAggregate aggregate = service.resolveAfterLockedBout(bout1);

    assertThat(aggregate.resolved()).isTrue();
    assertThat(aggregate.winnerSide()).isEqualTo(Side.AO);
    assertThat(aggregate.winType()).isEqualTo(WinType.HANTEI);
    assertThat(aggregate.reasonCode()).isEqualTo("TEAM_EXTRA_BOUT");
  }

  private Match regularBout(int order, Side winnerSide, int akaScore, int aoScore) {
    Match match = baseMatch(order);
    match.status = MatchStatus.LOCKED;
    match.winnerEntry = winnerSide == Side.AKA ? akaTeam : aoTeam;
    match.winType = WinType.POINTS;
    addParticipants(match);
    addState(match, akaScore, aoScore);
    groupMatches.add(match);
    return match;
  }

  private Match extraBout(int order, Side winnerSide, WinType winType) {
    Match match = baseMatch(order);
    match.teamExtraBout = true;
    match.status = MatchStatus.LOCKED;
    match.winnerEntry = winnerSide == Side.AKA ? akaTeam : aoTeam;
    match.winType = winType;
    addParticipants(match);
    addState(match, 0, 0);
    groupMatches.add(match);
    return match;
  }

  private Match baseMatch(int order) {
    Tournament tournament = Tournament.create();
    tournament.id = UUID.randomUUID();
    Category category = Category.create();
    category.id = UUID.randomUUID();
    category.tournament = tournament;
    category.name = "Team Kumite";
    category.discipline = CategoryDiscipline.TEAM_KUMITE;

    Match match = Match.create();
    match.id = UUID.randomUUID();
    match.tournament = tournament;
    match.category = category;
    match.mode = CategoryDiscipline.TEAM_KUMITE;
    match.matchNumber = order;
    match.roundName = "Final";
    match.roundNumber = 1;
    match.bracketPosition = 1;
    match.teamMatchGroupId = groupId;
    match.teamBoutOrder = order;
    return match;
  }

  private Entry entry() {
    Entry entry = Entry.create();
    entry.id = UUID.randomUUID();
    entry.teamId = UUID.randomUUID();
    return entry;
  }

  private void addParticipants(Match match) {
    MatchParticipant aka = participant(match, akaTeam, Side.AKA);
    MatchParticipant ao = participant(match, aoTeam, Side.AO);
    when(participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, Side.AKA)).thenReturn(Optional.of(aka));
    when(participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, Side.AO)).thenReturn(Optional.of(ao));
  }

  private MatchParticipant participant(Match match, Entry entry, Side side) {
    MatchParticipant participant = MatchParticipant.create();
    participant.id = UUID.randomUUID();
    participant.match = match;
    participant.entry = entry;
    participant.side = side;
    return participant;
  }

  private void addState(Match match, int akaScore, int aoScore) {
    KumiteMatchState state = KumiteMatchState.create();
    state.match = match;
    state.matchId = match.id;
    state.akaScore = akaScore;
    state.aoScore = aoScore;
    when(kumiteStates.findById(match.id)).thenReturn(Optional.of(state));
  }
}
