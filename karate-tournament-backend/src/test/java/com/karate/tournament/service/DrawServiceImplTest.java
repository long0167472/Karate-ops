package com.karate.tournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.request.DrawRequest;
import com.karate.tournament.dto.response.MatchResponse;
import com.karate.tournament.entity.Bracket;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.enums.BracketType;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.repository.BracketRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.rules.BracketRuleEngine;
import com.karate.tournament.service.impl.DrawServiceImpl;
import com.karate.tournament.web.ApiMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrawServiceImplTest {
  private CategoryService categoryService;
  private EntryRepository entries;
  private BracketRepository brackets;
  private MatchRepository matches;
  private MatchParticipantRepository matchParticipants;
  private KumiteMatchStateRepository kumiteStates;
  private ApiMapper mapper;
  private DrawServiceImpl service;
  private List<Match> savedMatches;
  private List<MatchParticipant> savedParticipants;
  private List<KumiteMatchState> savedStates;

  @BeforeEach
  void setUp() {
    categoryService = mock(CategoryService.class);
    entries = mock(EntryRepository.class);
    brackets = mock(BracketRepository.class);
    matches = mock(MatchRepository.class);
    matchParticipants = mock(MatchParticipantRepository.class);
    kumiteStates = mock(KumiteMatchStateRepository.class);
    mapper = mock(ApiMapper.class);
    savedMatches = new ArrayList<>();
    savedParticipants = new ArrayList<>();
    savedStates = new ArrayList<>();

    service = new DrawServiceImpl(
        categoryService,
        entries,
        brackets,
        matches,
        matchParticipants,
        kumiteStates,
        mock(PermissionService.class),
        new BracketRuleEngine(),
        mapper
    );
  }

  @Test
  void roundRobinDrawCreatesAllPairingsForThreeParticipants() {
    Category category = category();
    List<Entry> entryList = entries(category, 3);
    stubPersistence();
    stubCategoryDraw(category, entryList);

    var response = service.draw(category.id, new DrawRequest(BracketType.ROUND_ROBIN, false, null));

    assertThat(response.bracketSize()).isEqualTo(3);
    assertThat(response.entryCount()).isEqualTo(3);
    assertThat(savedMatches).hasSize(3);
    assertThat(savedParticipants).hasSize(6);
    assertThat(savedStates).hasSize(3)
        .allSatisfy(state -> {
          assertThat(state.durationMs).isEqualTo(120000);
          assertThat(state.remainingMs).isEqualTo(120000);
        });
    assertRoundRobinPairs(entryList, "0-1", "0-2", "1-2");
  }

  @Test
  void roundRobinDrawCreatesAllPairingsForFourParticipants() {
    Category category = category();
    List<Entry> entryList = entries(category, 4);
    stubPersistence();
    stubCategoryDraw(category, entryList);

    var response = service.draw(category.id, new DrawRequest(BracketType.ROUND_ROBIN, false, null));

    assertThat(response.bracketSize()).isEqualTo(4);
    assertThat(response.entryCount()).isEqualTo(4);
    assertThat(savedMatches).hasSize(6);
    assertThat(savedParticipants).hasSize(12);
    assertRoundRobinPairs(entryList, "0-1", "0-2", "0-3", "1-2", "1-3", "2-3");
  }

  @Test
  void poolDrawFailsWithBadRequestUntilPoolRulesAreImplemented() {
    Category category = category();
    stubCategoryOnly(category);

    assertThatThrownBy(() -> service.draw(category.id, new DrawRequest(BracketType.POOL, false, null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("POOL draw generation is not implemented yet");
  }

  private void stubCategoryDraw(Category category, List<Entry> entryList) {
    stubCategoryOnly(category);
    when(entries.findByCategory_IdAndDeletedAtIsNullOrderBySeedNoAscCreatedAtAsc(category.id)).thenReturn(entryList);
  }

  private void stubCategoryOnly(Category category) {
    when(categoryService.requireCategory(category.id)).thenReturn(category);
    when(matches.findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(category.id))
        .thenReturn(List.of())
        .thenReturn(savedMatches);
  }

  private void stubPersistence() {
    when(brackets.save(any(Bracket.class))).thenAnswer(invocation -> {
      Bracket bracket = invocation.getArgument(0, Bracket.class);
      bracket.id = UUID.randomUUID();
      return bracket;
    });
    when(matches.save(any(Match.class))).thenAnswer(invocation -> {
      Match match = invocation.getArgument(0, Match.class);
      match.id = UUID.randomUUID();
      savedMatches.add(match);
      return match;
    });
    when(matchParticipants.save(any(MatchParticipant.class))).thenAnswer(invocation -> {
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
    when(mapper.match(any(Match.class))).thenAnswer(invocation -> {
      Match match = invocation.getArgument(0, Match.class);
      return new MatchResponse(
          match.id,
          match.tournament.id,
          match.category.id,
          match.category.name,
          null,
          null,
          match.matchNumber,
          match.roundName,
          match.roundNumber,
          match.bracketPosition,
          match.status,
          match.scheduledAt,
          match.mode,
          null,
          null,
          null,
          List.of(),
          null,
          List.of(),
          List.of()
      );
    });
  }

  private Category category() {
    Tournament tournament = Tournament.create();
    tournament.id = UUID.randomUUID();
    Category category = Category.create();
    category.id = UUID.randomUUID();
    category.name = "Kumite Round Robin";
    category.tournament = tournament;
    category.discipline = CategoryDiscipline.KUMITE;
    category.matchDurationSeconds = 120;
    return category;
  }

  private List<Entry> entries(Category category, int count) {
    List<Entry> entryList = new ArrayList<>();
    for (int index = 0; index < count; index += 1) {
      Entry entry = Entry.create();
      entry.id = UUID.randomUUID();
      entry.category = category;
      entry.seedNo = index + 1;
      entry.createdAt = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index);
      entryList.add(entry);
    }
    return entryList;
  }

  private void assertRoundRobinPairs(List<Entry> entryList, String... expectedPairs) {
    assertThat(savedMatches)
        .extracting(match -> pairLabel(entryList, match))
        .containsExactly(expectedPairs);
    assertThat(savedMatches)
        .allSatisfy(match -> assertThat(participantsFor(match))
            .extracting(participant -> participant.side)
            .containsExactly(Side.AKA, Side.AO));
  }

  private String pairLabel(List<Entry> entryList, Match match) {
    List<MatchParticipant> participants = participantsFor(match);
    int akaIndex = entryList.indexOf(participants.get(0).entry);
    int aoIndex = entryList.indexOf(participants.get(1).entry);
    return akaIndex + "-" + aoIndex;
  }

  private List<MatchParticipant> participantsFor(Match match) {
    return savedParticipants.stream()
        .filter(participant -> participant.match == match)
        .toList();
  }
}
