package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BusinessConflictException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Bracket;
import com.karate.tournament.entity.enums.BracketStatus;
import com.karate.tournament.entity.enums.BracketType;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.repository.BracketRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.rules.BracketRuleEngine;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.DrawRequest;
import com.karate.tournament.dto.response.DrawResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {
  private final CategoryService categoryService;
  private final EntryRepository entries;
  private final BracketRepository brackets;
  private final MatchRepository matches;
  private final MatchParticipantRepository matchParticipants;
  private final KumiteMatchStateRepository kumiteStates;
  private final PermissionService permissions;
  private final BracketRuleEngine bracketRules;
  private final ApiMapper mapper;

  @Transactional
  public DrawResponse draw(UUID categoryId, DrawRequest request) {
    Category category = categoryService.requireCategory(categoryId);
    permissions.requireTournamentManage(category.tournament);
    if (!matches.findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(categoryId).isEmpty()) {
      throw new BusinessConflictException("Category already has generated matches");
    }

    List<Entry> entryList = new ArrayList<>(entries.findByCategory_IdAndDeletedAtIsNullOrderBySeedNoAscCreatedAtAsc(categoryId));
    entryList.removeIf(entry -> entry.status.name().equals("WITHDRAWN") || entry.status.name().equals("DISQUALIFIED"));
    if (entryList.size() < 2) {
      throw new BusinessConflictException("At least two active entries are required to draw a bracket");
    }
    boolean shuffle = request != null && Boolean.TRUE.equals(request.shuffle());
    if (shuffle) {
      Collections.shuffle(entryList);
    } else {
      entryList.sort(Comparator.comparing((Entry entry) -> entry.seedNo == null ? Integer.MAX_VALUE : entry.seedNo)
          .thenComparing(entry -> entry.createdAt));
    }

    int bracketSize = bracketRules.nextPowerOfTwo(entryList.size());
    int roundCount = bracketRules.roundCount(bracketSize);
    boolean repechage = request != null && request.enableRepechage() != null
        ? request.enableRepechage()
        : Boolean.TRUE.equals(category.repechageEnabled);
    Bracket bracket = Bracket.create();
    bracket.category = category;
    bracket.type = request == null || request.bracketType() == null ? (repechage ? BracketType.REPECHAGE : BracketType.SINGLE_ELIMINATION) : request.bracketType();
    bracket.size = bracketSize;
    bracket.status = BracketStatus.GENERATED;
    brackets.save(bracket);

    List<List<Match>> rounds = createMatches(category, bracket, bracketSize, roundCount);
    connectWinnerPaths(rounds);
    if (bracket.type == BracketType.REPECHAGE && roundCount >= 2) {
      List<Match> bronzeMatches = createBronzeMatches(category, bracket, rounds, roundCount);
      connectSemifinalLosersToBronze(rounds, bronzeMatches);
    }
    assignInitialEntries(rounds.get(0), entryList, bracketSize);
    autoAdvanceFirstRoundByes(rounds.get(0));

    category.status = "DRAWN";
    List<Match> generated = matches.findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(categoryId);
    return new DrawResponse(
        bracket.id,
        category.id,
        bracketSize,
        entryList.size(),
        generated.stream().map(mapper::match).toList()
    );
  }

  private List<List<Match>> createMatches(Category category, Bracket bracket, int bracketSize, int roundCount) {
    List<List<Match>> rounds = new ArrayList<>();
    int matchNumber = 1;
    int matchesInRound = bracketSize / 2;
    for (int round = 1; round <= roundCount; round += 1) {
      List<Match> roundMatches = new ArrayList<>();
      for (int position = 1; position <= matchesInRound; position += 1) {
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
    for (int roundIndex = 0; roundIndex < rounds.size() - 1; roundIndex += 1) {
      List<Match> currentRound = rounds.get(roundIndex);
      List<Match> nextRound = rounds.get(roundIndex + 1);
      for (int index = 0; index < currentRound.size(); index += 1) {
        Match current = currentRound.get(index);
        current.winnerNextMatch = nextRound.get(index / 2);
        current.winnerNextSide = index % 2 == 0 ? Side.AKA : Side.AO;
      }
    }
  }

  private List<Match> createBronzeMatches(Category category, Bracket bracket, List<List<Match>> rounds, int roundCount) {
    int matchNumber = rounds.stream().mapToInt(List::size).sum() + 1;
    List<Match> bronzeMatches = new ArrayList<>();
    for (int position = 1; position <= 2; position += 1) {
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
    for (int index = 0; index < semifinals.size() && index < 2; index += 1) {
      Match semifinal = semifinals.get(index);
      semifinal.loserNextMatch = bronzeMatches.get(index);
      semifinal.loserNextSide = Side.AKA;
    }
  }

  private void assignInitialEntries(List<Match> firstRound, List<Entry> entryList, int bracketSize) {
    for (int slot = 0; slot < bracketSize; slot += 1) {
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
}
