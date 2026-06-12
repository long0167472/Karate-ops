package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.service.TournamentPointsService;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.KataVote;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchAuditEvent;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.MatchScoreEvent;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.ScoreEventType;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.entity.CategoryResult;
import com.karate.tournament.repository.CategoryResultRepository;
import com.karate.tournament.repository.KataVoteRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchAuditEventRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.repository.MatchScoreEventRepository;
import com.karate.tournament.rules.KumiteRuleEngine;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteSnapshot;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.ConfirmResultRequest;
import com.karate.tournament.dto.request.MatchEventRequest;
import com.karate.tournament.dto.response.MatchResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
  private final MatchRepository matches;
  private final MatchParticipantRepository participants;
  private final KumiteMatchStateRepository kumiteStates;
  private final KataVoteRepository kataVotes;
  private final MatchScoreEventRepository scoreEvents;
  private final MatchAuditEventRepository auditEvents;
  private final CategoryResultRepository categoryResults;
  private final PermissionService permissions;
  private final KumiteRuleEngine kumiteRules;
  private final ApiMapper mapper;
  private final RealtimePublisher realtimePublisher;
  private final TournamentPointsService tournamentPointsService;

  @Transactional(readOnly = true)
  public List<MatchResponse> listByTournament(UUID tournamentId) {
    return matches.findByTournament_IdAndDeletedAtIsNullOrderByScheduledAtAscMatchNumberAsc(tournamentId)
        .stream()
        .peek(match -> permissions.requireViewTournament(match.tournament))
        .map(mapper::match)
        .toList();
  }

  @Transactional(readOnly = true)
  public MatchResponse get(UUID id) {
    Match match = requireMatch(id);
    permissions.requireViewTournament(match.tournament);
    return mapper.match(match);
  }

  @Transactional
  public MatchResponse recordEvent(UUID matchId, MatchEventRequest request) {
    Match match = requireMatch(matchId);
    permissions.requireTatamiOperate(match.tournament);
    applyEvent(match, request);
    MatchScoreEvent event = MatchScoreEvent.create();
    event.match = match;
    event.actorUserId = permissions.currentActor().userId();
    event.type = request.type();
    event.side = request.side();
    event.points = request.points();
    event.penaltyCode = request.penaltyCode();
    event.judgeNumber = request.judgeNumber();
    event.voteSide = request.voteSide();
    event.payloadJson = request.payloadJson();
    event.occurredAt = Instant.now();
    scoreEvents.save(event);
    MatchResponse response = mapper.match(match);
    realtimePublisher.publishMatch(response);
    return response;
  }

  @Transactional
  public MatchResponse confirmResult(UUID matchId, ConfirmResultRequest request) {
    Match match = requireMatch(matchId);
    permissions.requireTatamiOperate(match.tournament);
    MatchParticipant winnerParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(match.id, request.winnerSide())
        .orElseThrow(() -> new BusinessConflictException("Winner side has no participant"));
    Entry winnerEntry = winnerParticipant.entry;
    if (winnerEntry == null) {
      throw new BusinessConflictException("Winner side has no entry");
    }
    match.winnerEntry = winnerEntry;
    match.winnerAthlete = winnerEntry.athlete;
    match.winType = request.winType() == null ? WinType.MANUAL : request.winType();
    match.status = MatchStatus.LOCKED;
    saveResultEvent(match, request);
    advanceWinner(match, winnerEntry);
    advanceLoser(match, request.winnerSide());
    if (match.roundNumber != null) {
      tournamentPointsService.awardMatchPoints(match.id, winnerEntry.id, match.roundNumber);
    }
    recordMedalsIfReady(match, winnerEntry, request.winnerSide());
    MatchResponse response = mapper.match(match);
    realtimePublisher.publishMatch(response);
    if (match.winnerNextMatch != null) {
      realtimePublisher.publishMatch(mapper.match(match.winnerNextMatch));
    }
    if (match.loserNextMatch != null) {
      realtimePublisher.publishMatch(mapper.match(match.loserNextMatch));
    }
    return response;
  }

  public Match requireMatch(UUID id) {
    return matches.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
  }

  private void applyEvent(Match match, MatchEventRequest request) {
    switch (request.type()) {
      case SCORE_DELTA -> applyScoreDelta(match, request);
      case PENALTY -> applyPenalty(match, request);
      case SENSHU -> applySenshu(match, request);
      case TIMER_START -> applyTimerStart(match);
      case TIMER_STOP -> applyTimerStop(match);
      case TIMER_SET -> applyTimerSet(match, request);
      case KATA_VOTE -> applyKataVote(match, request);
      case STATUS_CHANGE -> {
        if (request.status() == null) throw new BadRequestException("status is required");
        match.status = request.status();
      }
      default -> {
        if (match.status == MatchStatus.SCHEDULED) match.status = MatchStatus.READY;
      }
    }
  }

  private void applyScoreDelta(Match match, MatchEventRequest request) {
    if (request.side() == null || request.points() == null) {
      throw new BadRequestException("side and points are required for SCORE_DELTA");
    }
    if (!List.of(-3, -2, -1, 1, 2, 3).contains(request.points())) {
      throw new BadRequestException("points must be -3, -2, -1, 1, 2, or 3");
    }
    KumiteMatchState state = requireKumiteState(match);
    if (request.side() == Side.AKA) {
      state.akaScore = Math.max(0, state.akaScore + request.points());
    } else {
      state.aoScore = Math.max(0, state.aoScore + request.points());
    }
    if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.READY) {
      match.status = MatchStatus.RUNNING;
    }
    applyKumiteSuggestion(match, state);
  }

  private void applyPenalty(Match match, MatchEventRequest request) {
    if (request.side() == null || request.penaltyCode() == null) {
      throw new BadRequestException("side and penaltyCode are required for PENALTY");
    }
    KumiteMatchState state = requireKumiteState(match);
    String code = request.penaltyCode().trim().toUpperCase();
    boolean aka = request.side() == Side.AKA;
    switch (code) {
      case "CHUI" -> {
        if (aka) state.akaChui = Math.min(3, request.points() == null ? state.akaChui + 1 : request.points());
        else state.aoChui = Math.min(3, request.points() == null ? state.aoChui + 1 : request.points());
      }
      case "HANSOKU_CHUI" -> {
        if (aka) state.akaHansokuChui = true;
        else state.aoHansokuChui = true;
      }
      case "HANSOKU" -> {
        if (aka) state.akaHansoku = true;
        else state.aoHansoku = true;
      }
      case "SHIKKAKU" -> {
        if (aka) state.akaShikkaku = true;
        else state.aoShikkaku = true;
      }
      case "KIKEN" -> {
        if (aka) state.akaKiken = true;
        else state.aoKiken = true;
      }
      default -> throw new BadRequestException("Unsupported penaltyCode: " + request.penaltyCode());
    }
    applyKumiteSuggestion(match, state);
  }

  private void applySenshu(Match match, MatchEventRequest request) {
    if (request.side() == null) throw new BadRequestException("side is required for SENSHU");
    KumiteMatchState state = requireKumiteState(match);
    state.akaSenshu = request.side() == Side.AKA;
    state.aoSenshu = request.side() == Side.AO;
  }

  private void applyTimerStart(Match match) {
    KumiteMatchState state = requireKumiteState(match);
    if (!state.timerRunning) {
      state.timerRunning = true;
      state.timerStartedAt = Instant.now();
    }
    match.status = MatchStatus.RUNNING;
  }

  private void applyTimerStop(Match match) {
    KumiteMatchState state = requireKumiteState(match);
    if (state.timerRunning && state.timerStartedAt != null) {
      long elapsedMs = java.time.Duration.between(state.timerStartedAt, Instant.now()).toMillis();
      state.remainingMs = Math.max(0, state.remainingMs - Math.toIntExact(Math.min(elapsedMs, Integer.MAX_VALUE)));
    }
    state.timerRunning = false;
    state.timerStartedAt = null;
    if (match.status == MatchStatus.RUNNING) {
      match.status = MatchStatus.PAUSED;
    }
  }

  private void applyTimerSet(Match match, MatchEventRequest request) {
    if (request.timerMs() == null) throw new BadRequestException("timerMs is required for TIMER_SET");
    KumiteMatchState state = requireKumiteState(match);
    state.remainingMs = Math.max(0, request.timerMs());
    applyKumiteSuggestion(match, state);
  }

  private void applyKataVote(Match match, MatchEventRequest request) {
    if (request.judgeNumber() == null || request.voteSide() == null) {
      throw new BadRequestException("judgeNumber and voteSide are required for KATA_VOTE");
    }
    KataVote vote = kataVotes.findByMatch_IdAndJudgeNumberAndDeletedAtIsNull(match.id, request.judgeNumber())
        .orElseGet(KataVote::create);
    vote.match = match;
    vote.judgeNumber = request.judgeNumber();
    vote.side = request.voteSide();
    kataVotes.save(vote);
    match.status = MatchStatus.VOTING;
  }

  private KumiteMatchState requireKumiteState(Match match) {
    if (!isKumite(match.mode)) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    return kumiteStates.findById(match.id).orElseGet(() -> {
      KumiteMatchState state = KumiteMatchState.create();
      state.match = match;
      return kumiteStates.save(state);
    });
  }

  private void applyKumiteSuggestion(Match match, KumiteMatchState state) {
    kumiteRules.suggestWinner(new KumiteSnapshot(
        state.akaScore,
        state.aoScore,
        state.akaSenshu,
        state.aoSenshu,
        state.akaHansoku,
        state.aoHansoku,
        state.akaShikkaku,
        state.aoShikkaku,
        state.akaKiken,
        state.aoKiken,
        state.remainingMs,
        state.timerRunning
    )).ifPresent(suggestion -> {
      if (suggestion.side() == null && suggestion.winType() == WinType.HANTEI) {
        match.status = MatchStatus.HANTEI;
      }
    });
  }

  private void saveResultEvent(Match match, ConfirmResultRequest request) {
    MatchScoreEvent scoreEvent = MatchScoreEvent.create();
    scoreEvent.match = match;
    scoreEvent.actorUserId = permissions.currentActor().userId();
    scoreEvent.type = ScoreEventType.RESULT_CONFIRMED;
    scoreEvent.side = request.winnerSide();
    scoreEvent.payloadJson = request.reason();
    scoreEvents.save(scoreEvent);

    MatchAuditEvent audit = MatchAuditEvent.create();
    audit.match = match;
    audit.actorUserId = permissions.currentActor().userId();
    audit.action = "RESULT_CONFIRMED";
    audit.reason = request.reason();
    auditEvents.save(audit);
  }

  private void advanceWinner(Match match, Entry winnerEntry) {
    if (match.winnerNextMatch == null || match.winnerNextSide == null) {
      return;
    }
    MatchParticipant nextParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(match.winnerNextMatch.id, match.winnerNextSide)
        .orElseGet(MatchParticipant::create);
    nextParticipant.match = match.winnerNextMatch;
    nextParticipant.side = match.winnerNextSide;
    nextParticipant.entry = winnerEntry;
    participants.save(nextParticipant);
    if (match.winnerNextMatch.status == MatchStatus.SCHEDULED) {
      match.winnerNextMatch.status = MatchStatus.READY;
    }
  }

  private void advanceLoser(Match match, Side winnerSide) {
    if (match.loserNextMatch == null || match.loserNextSide == null) {
      return;
    }
    Side loserSide = winnerSide == Side.AKA ? Side.AO : Side.AKA;
    MatchParticipant loserParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(match.id, loserSide)
        .orElse(null);
    if (loserParticipant == null || loserParticipant.entry == null) {
      return;
    }
    MatchParticipant nextParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(match.loserNextMatch.id, match.loserNextSide)
        .orElseGet(MatchParticipant::create);
    nextParticipant.match = match.loserNextMatch;
    nextParticipant.side = match.loserNextSide;
    nextParticipant.entry = loserParticipant.entry;
    participants.save(nextParticipant);
    if (match.loserNextMatch.status == MatchStatus.SCHEDULED) {
      match.loserNextMatch.status = MatchStatus.READY;
    }
  }

  private void recordMedalsIfReady(Match match, Entry winnerEntry, Side winnerSide) {
    if (match.roundName == null) {
      return;
    }
    if (match.roundName.equalsIgnoreCase("Final")) {
      saveCategoryResult(match, winnerEntry, 1, "GOLD");
      Side loserSide = winnerSide == Side.AKA ? Side.AO : Side.AKA;
      participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, loserSide)
          .map(participant -> participant.entry)
          .ifPresent(loser -> saveCategoryResult(match, loser, 2, "SILVER"));
      return;
    }
    if (match.roundName.startsWith("Bronze Medal")) {
      saveCategoryResult(match, winnerEntry, 3, "BRONZE");
    }
  }

  private void saveCategoryResult(Match match, Entry entry, int placement, String medal) {
    if (categoryResults.existsByCategory_IdAndEntry_IdAndDeletedAtIsNull(match.category.id, entry.id)) {
      return;
    }
    CategoryResult result = CategoryResult.create();
    result.category = match.category;
    result.entry = entry;
    result.athlete = entry.athlete;
    result.teamId = entry.teamId;
    result.placement = placement;
    result.medal = medal;
    categoryResults.save(result);
  }

  private boolean isKumite(CategoryDiscipline discipline) {
    return discipline == CategoryDiscipline.KUMITE || discipline == CategoryDiscipline.TEAM_KUMITE;
  }
}
