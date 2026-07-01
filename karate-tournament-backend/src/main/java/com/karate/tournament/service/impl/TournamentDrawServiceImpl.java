package com.karate.tournament.service.impl;

import lombok.RequiredArgsConstructor;
import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.response.TournamentDrawSummaryResponse;
import com.karate.tournament.dto.response.TournamentDrawSummaryResponse.CategoryDrawResponse;
import com.karate.tournament.entity.Bracket;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.enums.BracketStatus;
import com.karate.tournament.entity.enums.BracketType;
import com.karate.tournament.entity.enums.BtcApprovalStatus;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.repository.BracketRepository;
import com.karate.tournament.repository.CategoryRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.rules.BracketRuleEngine;
import com.karate.tournament.service.TournamentDrawService;
import com.karate.tournament.service.TournamentService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TournamentDrawServiceImpl implements TournamentDrawService {

  private final TournamentService tournamentService;
  private final CategoryRepository categories;
  private final EntryRepository entries;
  private final BracketRepository brackets;
  private final MatchRepository matches;
  private final MatchParticipantRepository matchParticipants;
  private final KumiteMatchStateRepository kumiteStates;
  private final PermissionService permissions;
  private final BracketRuleEngine bracketRules;

  @Override
  public TournamentDrawSummaryResponse generateDraw(UUID tournamentId) {
    Tournament tournament = tournamentService.requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    if (tournament.step != 2) {
      throw new BusinessConflictException("Tournament must be at step 2 (athlete approval) to generate a draw");
    }

    List<Category> categoryList = categories.findByTournament_IdAndDeletedAtIsNullOrderByNameAsc(tournamentId);
    List<CategoryDrawResponse> responses = new ArrayList<>();

    for (Category category : categoryList) {
      List<Entry> approvedEntries = entries.findByCategory_IdAndDeletedAtIsNullOrderBySeedNoAscCreatedAtAsc(category.id)
          .stream()
          .filter(e -> e.btcApprovalStatus == BtcApprovalStatus.APPROVED)
          .toList();

      int count = approvedEntries.size();
      if (count < 2) {
        responses.add(new CategoryDrawResponse(category.id, category.name, count, 0, false));
        continue;
      }

      int bracketSize = bracketRules.nextPowerOfTwo(count);
      int roundCount = bracketRules.roundCount(bracketSize);

      List<Entry> seeded = spreadByClub(approvedEntries);

      Bracket bracket = Bracket.create();
      bracket.category = category;
      bracket.type = Boolean.TRUE.equals(category.repechageEnabled) ? BracketType.REPECHAGE : BracketType.SINGLE_ELIMINATION;
      bracket.size = bracketSize;
      bracket.status = BracketStatus.GENERATED;
      brackets.save(bracket);

      List<List<Match>> rounds = createMatches(category, bracket, bracketSize, roundCount);
      connectWinnerPaths(rounds);
      if (bracket.type == BracketType.REPECHAGE && roundCount >= 2) {
        List<Match> bronzeMatches = createBronzeMatches(category, bracket, rounds, roundCount);
        connectSemifinalLosersToBronze(rounds, bronzeMatches);
      }
      assignInitialEntries(rounds.get(0), seeded, bracketSize);
      autoAdvanceFirstRoundByes(rounds.get(0));

      category.status = "DRAWN";
      responses.add(new CategoryDrawResponse(category.id, category.name, count, bracketSize, true));
    }

    tournament.step = 3;
    return new TournamentDrawSummaryResponse(responses);
  }

  @Override
  public void clearDraw(UUID tournamentId) {
    Tournament tournament = tournamentService.requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);

    List<Category> categoryList = categories.findByTournament_IdAndDeletedAtIsNullOrderByNameAsc(tournamentId);
    for (Category category : categoryList) {
      List<Match> matchList = matches.findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(category.id);
      for (Match match : matchList) {
        List<MatchParticipant> participants = matchParticipants.findByMatch_IdAndDeletedAtIsNullOrderBySideAsc(match.id);
        for (MatchParticipant participant : participants) {
          participant.softDelete();
        }
        match.softDelete();
      }
      List<Bracket> bracketList = brackets.findByCategory_IdAndDeletedAtIsNullOrderByCreatedAtDesc(category.id);
      for (Bracket bracket : bracketList) {
        bracket.softDelete();
      }
      if ("DRAWN".equals(category.status)) {
        category.status = "DRAFT";
      }
    }

    tournament.step = 2;
  }

  @Override
  @Transactional(readOnly = true)
  public TournamentDrawSummaryResponse getDraw(UUID tournamentId) {
    Tournament tournament = tournamentService.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);

    List<Category> categoryList = categories.findByTournament_IdAndDeletedAtIsNullOrderByNameAsc(tournamentId);
    List<CategoryDrawResponse> responses = new ArrayList<>();

    for (Category category : categoryList) {
      long approvedCount = entries.findByCategory_IdAndDeletedAtIsNullOrderBySeedNoAscCreatedAtAsc(category.id)
          .stream()
          .filter(e -> e.btcApprovalStatus == BtcApprovalStatus.APPROVED)
          .count();

      List<Bracket> bracketList = brackets.findByCategory_IdAndDeletedAtIsNullOrderByCreatedAtDesc(category.id);
      boolean hasActiveDraw = !bracketList.isEmpty();
      int bracketSize = hasActiveDraw ? bracketList.get(0).size : 0;

      responses.add(new CategoryDrawResponse(
          category.id,
          category.name,
          (int) approvedCount,
          bracketSize,
          hasActiveDraw
      ));
    }

    return new TournamentDrawSummaryResponse(responses);
  }

  @Override
  public void startTournament(UUID tournamentId) {
    Tournament tournament = tournamentService.requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    if (tournament.step != 3) {
      throw new BusinessConflictException("Tournament must be at step 3 (draw complete) to start");
    }
    tournament.step = 4;
    tournament.status = TournamentStatus.RUNNING;
  }

  // --- Sigma club-spread interleaving ---

  private List<Entry> spreadByClub(List<Entry> entryList) {
    // Group entries by club (organization id from tournamentParticipant.organization)
    Map<UUID, List<Entry>> byClub = new LinkedHashMap<>();
    for (Entry entry : entryList) {
      UUID orgId = entry.tournamentParticipant != null && entry.tournamentParticipant.organization != null
          ? entry.tournamentParticipant.organization.id
          : null;
      byClub.computeIfAbsent(orgId, k -> new ArrayList<>()).add(entry);
    }

    // Shuffle within each club for randomness
    for (List<Entry> clubEntries : byClub.values()) {
      Collections.shuffle(clubEntries);
    }

    // Interleave: A1, B1, C1, A2, B2, A3 ...
    List<List<Entry>> clubLists = new ArrayList<>(byClub.values());
    List<Entry> result = new ArrayList<>();
    int maxSize = clubLists.stream().mapToInt(List::size).max().orElse(0);
    for (int round = 0; round < maxSize; round++) {
      for (List<Entry> club : clubLists) {
        if (round < club.size()) {
          result.add(club.get(round));
        }
      }
    }
    return result;
  }

  // --- Match creation (mirrors DrawServiceImpl) ---

  private List<List<Match>> createMatches(Category category, Bracket bracket, int bracketSize, int roundCount) {
    List<List<Match>> rounds = new ArrayList<>();
    int matchNumber = 1;
    int matchesInRound = bracketSize / 2;
    for (int round = 1; round <= roundCount; round++) {
      List<Match> roundMatches = new ArrayList<>();
      for (int position = 1; position <= matchesInRound; position++) {
        Match match = Match.create();
        match.tournament = category.tournament;
        match.category = category;
        match.bracket = bracket;
        match.matchNumber = matchNumber++;
        match.roundNumber = round;
        match.roundName = bracketRules.roundName(round, roundCount);
        match.bracketPosition = position;
        match.status = MatchStatus.SCHEDULED;
        match.mode = category.discipline;
        matches.save(match);
        if (isKumite(match.mode)) {
          KumiteMatchState state = KumiteMatchState.create();
          state.match = match;
          initializeDuration(category, state);
          kumiteStates.save(state);
        }
        roundMatches.add(match);
      }
      rounds.add(roundMatches);
      matchesInRound /= 2;
    }
    return rounds;
  }

  private void connectWinnerPaths(List<List<Match>> rounds) {
    for (int roundIndex = 0; roundIndex < rounds.size() - 1; roundIndex++) {
      List<Match> currentRound = rounds.get(roundIndex);
      List<Match> nextRound = rounds.get(roundIndex + 1);
      for (int index = 0; index < currentRound.size(); index++) {
        Match current = currentRound.get(index);
        current.winnerNextMatch = nextRound.get(index / 2);
        current.winnerNextSide = bracketRules.sideForSlot(index);
      }
    }
  }

  private List<Match> createBronzeMatches(Category category, Bracket bracket, List<List<Match>> rounds, int roundCount) {
    int matchNumber = rounds.stream().mapToInt(List::size).sum() + 1;
    List<Match> bronzeMatches = new ArrayList<>();
    for (int position = 1; position <= 2; position++) {
      Match match = Match.create();
      match.tournament = category.tournament;
      match.category = category;
      match.bracket = bracket;
      match.matchNumber = matchNumber++;
      match.roundNumber = roundCount + 1;
      match.roundName = "Bronze Medal " + position;
      match.bracketPosition = position;
      match.status = MatchStatus.SCHEDULED;
      match.mode = category.discipline;
      matches.save(match);
      if (isKumite(match.mode)) {
        KumiteMatchState state = KumiteMatchState.create();
        state.match = match;
        initializeDuration(category, state);
        kumiteStates.save(state);
      }
      bronzeMatches.add(match);
    }
    return bronzeMatches;
  }

  private void connectSemifinalLosersToBronze(List<List<Match>> rounds, List<Match> bronzeMatches) {
    if (rounds.size() < 2 || bronzeMatches.size() < 2) {
      return;
    }
    List<Match> semifinals = rounds.get(rounds.size() - 2);
    for (int index = 0; index < semifinals.size() && index < 2; index++) {
      Match semifinal = semifinals.get(index);
      semifinal.loserNextMatch = bronzeMatches.get(index);
      semifinal.loserNextSide = bracketRules.sideForSlot(0);
    }
  }

  private void assignInitialEntries(List<Match> firstRound, List<Entry> entryList, int bracketSize) {
    for (int slot = 0; slot < bracketSize; slot++) {
      if (slot >= entryList.size()) {
        continue;
      }
      Match match = firstRound.get(slot / 2);
      MatchParticipant participant = MatchParticipant.create();
      participant.match = match;
      participant.entry = entryList.get(slot);
      participant.side = bracketRules.sideForSlot(slot);
      matchParticipants.save(participant);
    }
  }

  private void autoAdvanceFirstRoundByes(List<Match> firstRound) {
    for (Match match : firstRound) {
      List<MatchParticipant> participants = matchParticipants.findByMatch_IdAndDeletedAtIsNullOrderBySideAsc(match.id);
      if (participants.size() == 1 && match.winnerNextMatch != null) {
        Entry winner = participants.get(0).entry;
        match.winnerEntry = winner;
        match.winnerAthlete = winner == null ? null : winner.athlete;
        match.winType = WinType.BYE;
        match.status = MatchStatus.COMPLETED;
        MatchParticipant nextParticipant = MatchParticipant.create();
        nextParticipant.match = match.winnerNextMatch;
        nextParticipant.entry = winner;
        nextParticipant.side = match.winnerNextSide;
        matchParticipants.save(nextParticipant);
      }
    }
  }

  private boolean isKumite(CategoryDiscipline discipline) {
    return discipline == CategoryDiscipline.KUMITE || discipline == CategoryDiscipline.TEAM_KUMITE;
  }

  private void initializeDuration(Category category, KumiteMatchState state) {
    int durationMs = Math.max(30, category.matchDurationSeconds == null ? 180 : category.matchDurationSeconds) * 1000;
    state.durationMs = durationMs;
    state.remainingMs = durationMs;
  }
}
