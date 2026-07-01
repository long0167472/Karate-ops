package com.karate.tournament.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.service.TeamKumiteAggregate;
import com.karate.tournament.service.TournamentPointsService;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.BusinessConflictException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.CategoryResult;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.KataVote;
import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchAuditEvent;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.MatchScoreEvent;
import com.karate.tournament.entity.Tatami;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.BracketType;
import com.karate.tournament.entity.enums.EntryStatus;
import com.karate.tournament.entity.enums.KumitePenaltyCategory;
import com.karate.tournament.entity.enums.KumitePenaltyLevel;
import com.karate.tournament.entity.enums.KumitePenaltyReasonCode;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.MedicalOutcome;
import com.karate.tournament.entity.enums.MedicalStatus;
import com.karate.tournament.entity.enums.ScoreEventType;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.VideoReviewResolution;
import com.karate.tournament.entity.enums.VideoReviewStatus;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.repository.CategoryResultRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.KataVoteRepository;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchAuditEventRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.repository.MatchScoreEventRepository;
import com.karate.tournament.rules.KumiteRuleEngine;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteDecision;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteMatchContext;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteMatchFormat;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteRulesProfile;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteSnapshot;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.ConfirmResultRequest;
import com.karate.tournament.dto.request.MatchEventRequest;
import com.karate.tournament.dto.response.MatchResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
  private static final List<MatchStatus> TATAMI_LIVE_STATUSES = List.copyOf(EnumSet.of(
      MatchStatus.READY,
      MatchStatus.RUNNING,
      MatchStatus.PAUSED,
      MatchStatus.REVIEW,
      MatchStatus.HANTEI,
      MatchStatus.HIKIWAKE,
      MatchStatus.RESULT_PENDING_CONFIRMATION,
      MatchStatus.VOTING
  ));

  private final MatchRepository matches;
  private final MatchParticipantRepository participants;
  private final KumiteMatchStateRepository kumiteStates;
  private final KataVoteRepository kataVotes;
  private final MatchScoreEventRepository scoreEvents;
  private final MatchAuditEventRepository auditEvents;
  private final CategoryResultRepository categoryResults;
  private final EntryRepository entries;
  private final PermissionService permissions;
  private final KumiteRuleEngine kumiteRules;
  private final ApiMapper mapper;
  private final RealtimePublisher realtimePublisher;
  private final TournamentPointsService tournamentPointsService;
  private final TeamKumiteResolutionService teamKumiteResolutionService;
  private final ObjectMapper objectMapper;

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
    KumiteMatchState state = isKumite(match.mode) ? requireKumiteState(match) : null;
    if (state != null) {
      expireMedicalIfNeeded(match, state);
    }
    validateEventAllowed(match, request, state);
    Side videoReviewRequestingSide = state == null ? null : state.videoReviewActiveSide;
    applyEvent(match, request, state);
    MatchScoreEvent event = MatchScoreEvent.create();
    event.match = match;
    event.actorUserId = permissions.currentActor().userId();
    event.type = request.type();
    event.side = request.side();
    event.points = request.points();
    event.penaltyCode = effectivePenaltyCode(request);
    event.judgeNumber = request.judgeNumber();
    event.voteSide = request.voteSide();
    event.payloadJson = auditPayloadJson(request, videoReviewRequestingSide);
    event.occurredAt = Instant.now();
    scoreEvents.save(event);
    if (state != null) {
      recomputeKumiteState(match, state);
    }
    MatchResponse response = mapper.match(match);
    realtimePublisher.publishMatch(response);
    return response;
  }

  @Transactional
  public MatchResponse confirmResult(UUID matchId, ConfirmResultRequest request) {
    Match match = requireMatch(matchId);
    permissions.requireTatamiOperate(match.tournament);
    KumiteMatchState state = isKumite(match.mode) ? requireKumiteState(match) : null;
    if (state != null) {
      expireMedicalIfNeeded(match, state);
    }
    if (match.status == MatchStatus.LOCKED || match.status == MatchStatus.CANCELLED) {
      throw new BusinessConflictException("Cannot confirm result on a " + match.status + " match");
    }
    if (match.status == MatchStatus.HANTEI) {
      throw new BusinessConflictException("Use HANTEI_DECISION before confirming the result");
    }
    if (state != null && isHikiwakeConfirmation(match, state, request)) {
      confirmHikiwake(match, state, request);
      TeamKumiteAggregate teamAggregate = resolveTeamKumiteAggregateIfNeeded(match);
      advanceTeamKumiteAggregateIfResolved(teamAggregate);
      MatchResponse response = mapper.match(match);
      realtimePublisher.publishMatch(response);
      publishCreatedExtraBout(teamAggregate);
      publishTeamAggregateNextMatches(teamAggregate);
      publishTatamiRollForward(match);
      return response;
    }
    if (request.winnerSide() == null) {
      throw new BadRequestException("winnerSide is required unless confirming HIKIWAKE");
    }
    MatchParticipant winnerParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(match.id, request.winnerSide())
        .orElseThrow(() -> new BusinessConflictException("Winner side has no participant"));
    Entry winnerEntry = winnerParticipant.entry;
    if (winnerEntry == null) {
      throw new BusinessConflictException("Winner side has no entry");
    }
    if (state != null && match.status == MatchStatus.RESULT_PENDING_CONFIRMATION && state.decisionConfirmable) {
      if (state.decisionWinnerSide == null || state.decisionWinnerSide != request.winnerSide()) {
        throw new BusinessConflictException("Winner side does not match the backend decision");
      }
    }

    match.winnerEntry = winnerEntry;
    match.winnerAthlete = winnerEntry.athlete;
    match.winType = resolveWinType(match, state, request.winnerSide(), request.winType());
    match.status = MatchStatus.LOCKED;
    if (state != null) {
      state.decisionFrozen = false;
      state.decisionConfirmable = false;
      state.videoReviewStatus = VideoReviewStatus.IDLE;
      state.videoReviewActiveSide = null;
      state.medicalStatus = MedicalStatus.IDLE;
      state.medicalInjuredSide = null;
      state.medicalStartedAt = null;
      state.medicalDeadlineAt = null;
    }
    saveResultEvent(match, request);
    TeamKumiteAggregate teamAggregate = resolveTeamKumiteAggregateIfNeeded(match);
    if (teamAggregate == null) {
      advanceWinner(match, winnerEntry);
      advanceLoser(match, request.winnerSide());
      if (match.roundNumber != null) {
        tournamentPointsService.awardMatchPoints(match.id, winnerEntry.id, match.roundNumber);
      }
      recordMedalsIfReady(match, winnerEntry, request.winnerSide());
    } else {
      advanceTeamKumiteAggregateIfResolved(teamAggregate);
    }
    clearTatamiCurrentMatchIfLocked(match);
    MatchResponse response = mapper.match(match);
    realtimePublisher.publishMatch(response);
    publishCreatedExtraBout(teamAggregate);
    publishNextMatches(match, teamAggregate);
    publishTatamiRollForward(match);
    return response;
  }

  public Match requireMatch(UUID id) {
    return matches.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + id));
  }

  private void applyEvent(Match match, MatchEventRequest request, KumiteMatchState state) {
    switch (request.type()) {
      case SCORE_DELTA -> applyScoreDelta(match, state, request.side(), request.points());
      case SCORE_EXCHANGE -> applyScoreExchange(match, state, request);
      case PENALTY -> applyPenalty(match, state, request);
      case TIMER_START -> applyTimerStart(match, state);
      case TIMER_STOP -> applyTimerStop(match, state);
      case TIMER_SET -> applyTimerSet(match, state, request.timerMs());
      case KATA_VOTE -> applyKataVote(match, request);
      case STATUS_CHANGE -> applyStatusChange(match, state, request.status());
      case HANTEI_DECISION -> applyHanteiDecision(match, state, request.side());
      case VIDEO_REVIEW_REQUEST -> applyVideoReviewRequest(match, state, request.side());
      case VIDEO_REVIEW_RESOLVE -> applyVideoReviewResolve(match, state, request);
      case MEDICAL_START -> applyMedicalStart(match, state, request.side());
      case MEDICAL_RESOLVE -> applyMedicalResolve(match, state, request.medicalOutcome());
      case SENSHU_REVOKE -> applySenshuRevoke(match, state, request.side(), request.penaltyReasonCode());
      case SENSHU -> throw new BadRequestException("Senshu is computed by backend scoring and revoke events");
      case RESULT_CONFIRMED -> throw new BadRequestException("Use /api/matches/{id}/result to confirm a result");
      case VR, KATA_REVEAL, MANUAL_CORRECTION -> throw new BadRequestException(request.type() + " is not supported in the WKF tatami workflow");
      default -> throw new BadRequestException("Unsupported event type: " + request.type());
    }
  }

  private void applyScoreDelta(Match match, KumiteMatchState state, Side side, Integer points) {
    if (side == null || points == null) {
      throw new BadRequestException("side and points are required for SCORE_DELTA");
    }
    validateScoreDelta(points);
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (side == Side.AKA) {
      state.akaScore = Math.max(0, state.akaScore + points);
    } else {
      state.aoScore = Math.max(0, state.aoScore + points);
    }
    if (points > 0 && state.senshuHolder == null && !state.senshuReawardBlocked) {
      awardSenshu(state, side, Instant.now());
    }
    if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.READY) {
      match.status = MatchStatus.RUNNING;
    }
  }

  private void applyScoreExchange(Match match, KumiteMatchState state, MatchEventRequest request) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    ScoreExchange exchange = parseScoreExchange(request);
    if (exchange.akaPoints() == 0 && exchange.aoPoints() == 0) {
      throw new BadRequestException("SCORE_EXCHANGE requires at least one side delta");
    }
    applyExchangeDelta(state, Side.AKA, exchange.akaPoints());
    applyExchangeDelta(state, Side.AO, exchange.aoPoints());

    Side onlyPositiveScorer = onlyPositiveScorer(exchange);
    if (onlyPositiveScorer != null && state.senshuHolder == null && !state.senshuReawardBlocked) {
      awardSenshu(state, onlyPositiveScorer, Instant.now());
    }
    if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.READY) {
      match.status = MatchStatus.RUNNING;
    }
  }

  private void applyExchangeDelta(KumiteMatchState state, Side side, int points) {
    if (points == 0) {
      return;
    }
    validateScoreDelta(points);
    if (side == Side.AKA) {
      state.akaScore = Math.max(0, state.akaScore + points);
    } else {
      state.aoScore = Math.max(0, state.aoScore + points);
    }
  }

  private void validateScoreDelta(int points) {
    if (!List.of(-3, -2, -1, 1, 2, 3).contains(points)) {
      throw new BadRequestException("points must be -3, -2, -1, 1, 2, or 3");
    }
  }

  private ScoreExchange parseScoreExchange(MatchEventRequest request) {
    if (request.payloadJson() == null || request.payloadJson().isBlank()) {
      throw new BadRequestException("payloadJson is required for SCORE_EXCHANGE");
    }
    try {
      JsonNode root = objectMapper.readTree(request.payloadJson());
      JsonNode deltas = root.path("deltas");
      int akaPoints = firstInt(root, deltas, "akaPoints", "AKA", "aka");
      int aoPoints = firstInt(root, deltas, "aoPoints", "AO", "ao");
      String exchangeId = request.exchangeId();
      if ((exchangeId == null || exchangeId.isBlank()) && root.hasNonNull("exchangeId")) {
        exchangeId = root.path("exchangeId").asText();
      }
      return new ScoreExchange(exchangeId, akaPoints, aoPoints);
    } catch (JsonProcessingException ex) {
      throw new BadRequestException("payloadJson must be valid SCORE_EXCHANGE JSON");
    }
  }

  private int firstInt(JsonNode root, JsonNode nested, String... fieldNames) {
    for (String fieldName : fieldNames) {
      if (root.hasNonNull(fieldName)) {
        return root.path(fieldName).asInt();
      }
      if (!nested.isMissingNode() && nested.hasNonNull(fieldName)) {
        return nested.path(fieldName).asInt();
      }
    }
    return 0;
  }

  private Side onlyPositiveScorer(ScoreExchange exchange) {
    boolean akaPositive = exchange.akaPoints() > 0;
    boolean aoPositive = exchange.aoPoints() > 0;
    if (akaPositive == aoPositive) {
      return null;
    }
    return akaPositive ? Side.AKA : Side.AO;
  }

  private void applyPenalty(Match match, KumiteMatchState state, MatchEventRequest request) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (request.side() == null) {
      throw new BadRequestException("side is required for PENALTY");
    }
    PenaltyCommand penalty = normalizePenalty(request);
    if (penalty.directOutcome() == WinType.KIKEN) {
      if (penalty.side() == Side.AKA) state.akaKiken = true;
      else state.aoKiken = true;
      return;
    }
    if (penalty.directOutcome() == WinType.SHIKKAKU) {
      if (penalty.side() == Side.AKA) state.akaShikkaku = true;
      else state.aoShikkaku = true;
      return;
    }
    applyPenaltyLevel(state, penalty.side(), penalty.level(), penalty.reasonCode());
  }

  private void applyTimerStart(Match match, KumiteMatchState state) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (!state.timerRunning) {
      state.timerRunning = true;
      state.timerStartedAt = Instant.now();
    }
    if (match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.READY || match.status == MatchStatus.PAUSED) {
      match.status = MatchStatus.RUNNING;
    }
  }

  private void applyTimerStop(Match match, KumiteMatchState state) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    stopTimer(state, Instant.now());
    if (match.status == MatchStatus.RUNNING) {
      match.status = MatchStatus.PAUSED;
    }
  }

  private void applyTimerSet(Match match, KumiteMatchState state, Integer timerMs) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (timerMs == null) {
      throw new BadRequestException("timerMs is required for TIMER_SET");
    }
    state.remainingMs = Math.max(0, timerMs);
    state.timerRunning = false;
    state.timerStartedAt = null;
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

  private void applyStatusChange(Match match, KumiteMatchState state, MatchStatus status) {
    if (status == null) {
      throw new BadRequestException("status is required");
    }
    if (status == MatchStatus.REVIEW && state != null) {
      rememberLastLiveStatus(match, state);
      clearDecision(state);
    }
    if (status == MatchStatus.REVIEW && state != null && match.status == MatchStatus.RUNNING) {
      stopTimer(state, Instant.now());
    }
    match.status = status;
  }

  private void applyHanteiDecision(Match match, KumiteMatchState state, Side side) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (side == null) {
      throw new BadRequestException("side is required for HANTEI_DECISION");
    }
    stopTimer(state, Instant.now());
    setDecision(
        state,
        new KumiteDecision(side, WinType.HANTEI, "HANTEI_DECISION", "Referee decision recorded", true, true)
    );
    match.status = MatchStatus.RESULT_PENDING_CONFIRMATION;
  }

  private void applyVideoReviewRequest(Match match, KumiteMatchState state, Side side) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (side == null) {
      throw new BadRequestException("side is required for VIDEO_REVIEW_REQUEST");
    }
    if (!videoReviewCardAvailable(state, side)) {
      throw new BusinessConflictException(side + " has no video review card available");
    }
    rememberLastLiveStatus(match, state);
    stopTimer(state, Instant.now());
    state.videoReviewStatus = VideoReviewStatus.REQUESTED;
    state.videoReviewActiveSide = side;
    match.status = MatchStatus.REVIEW;
  }

  private void applyVideoReviewResolve(Match match, KumiteMatchState state, MatchEventRequest request) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (request.resolution() == null) {
      throw new BadRequestException("resolution is required for VIDEO_REVIEW_RESOLVE");
    }
    Side requestingSide = state.videoReviewActiveSide;
    if (requestingSide == null) {
      throw new BusinessConflictException("No active video review request");
    }
    switch (request.resolution()) {
      case DENIED -> {
        consumeVideoReviewCard(state, requestingSide);
        state.videoReviewLastResolution = VideoReviewResolution.DENIED;
      }
      case AWARD_SCORE -> {
        Side resolutionSide = request.resolutionSide() == null ? requestingSide : request.resolutionSide();
        int resolutionPoints = normalizeResolutionPoints(request.resolutionPoints());
        applyScoreDelta(match, state, resolutionSide, resolutionPoints);
        state.videoReviewLastResolution = VideoReviewResolution.AWARD_SCORE;
      }
      case TORIMASEN -> {
        Side resolutionSide = request.resolutionSide() == null ? requestingSide : request.resolutionSide();
        int resolutionPoints = normalizeResolutionPoints(request.resolutionPoints());
        applyScoreDelta(match, state, resolutionSide, -resolutionPoints);
        if (state.senshuHolder == resolutionSide) {
          revokeSenshu(state, resolutionSide, "TORIMASEN", Instant.now(), true);
        }
        state.videoReviewLastResolution = VideoReviewResolution.TORIMASEN;
      }
      case REVOKE_SENSHU -> {
        Side resolutionSide = request.resolutionSide() == null ? state.senshuHolder : request.resolutionSide();
        if (resolutionSide == null) {
          throw new BadRequestException("resolutionSide is required when no senshu holder exists");
        }
        revokeSenshu(state, resolutionSide, "VIDEO_REVIEW_REVOKE_SENSHU", Instant.now(), true);
        state.videoReviewLastResolution = VideoReviewResolution.REVOKE_SENSHU;
      }
      case MIENAI -> state.videoReviewLastResolution = VideoReviewResolution.MIENAI;
      case TECHNICAL_PROBLEM -> state.videoReviewLastResolution = VideoReviewResolution.TECHNICAL_PROBLEM;
    }
    state.videoReviewStatus = VideoReviewStatus.IDLE;
    state.videoReviewActiveSide = null;
    if (match.status == MatchStatus.REVIEW) {
      match.status = MatchStatus.PAUSED;
    }
  }

  private void applyMedicalStart(Match match, KumiteMatchState state, Side side) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (side == null) {
      throw new BadRequestException("side is required for MEDICAL_START");
    }
    KumiteRulesProfile profile = kumiteRules.profileFor(match);
    rememberLastLiveStatus(match, state);
    stopTimer(state, Instant.now());
    state.medicalStatus = MedicalStatus.ACTIVE;
    state.medicalInjuredSide = side;
    state.medicalStartedAt = Instant.now();
    state.medicalDeadlineAt = state.medicalStartedAt.plusSeconds(profile.medicalCountdownSeconds());
    state.medicalLastOutcome = null;
    match.status = MatchStatus.REVIEW;
  }

  private void applyMedicalResolve(Match match, KumiteMatchState state, MedicalOutcome outcome) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    if (outcome == null) {
      throw new BadRequestException("medicalOutcome is required for MEDICAL_RESOLVE");
    }
    Side injuredSide = state.medicalInjuredSide;
    if (injuredSide == null) {
      throw new BusinessConflictException("No active medical workflow");
    }
    state.medicalLastOutcome = outcome;
    state.medicalStatus = MedicalStatus.IDLE;
    state.medicalStartedAt = null;
    state.medicalDeadlineAt = null;
    state.medicalInjuredSide = null;
    if (outcome == MedicalOutcome.UNFIT_TEN_SECOND_RULE) {
      Side winnerSide = injuredSide == Side.AKA ? Side.AO : Side.AKA;
      setDecision(state, new KumiteDecision(
          winnerSide,
          WinType.TEN_SECOND_RULE,
          "TEN_SECOND_RULE",
          "10 second rule",
          true,
          true
      ));
      match.status = MatchStatus.RESULT_PENDING_CONFIRMATION;
      propagateTenSecondRuleWithdrawal(match, injuredSide);
      return;
    }
    if (match.status == MatchStatus.REVIEW) {
      match.status = MatchStatus.PAUSED;
    }
  }

  private void applySenshuRevoke(Match match, KumiteMatchState state, Side side, String reasonCode) {
    if (state == null) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    Side effectiveSide = side == null ? state.senshuHolder : side;
    if (effectiveSide == null) {
      throw new BusinessConflictException("No senshu holder to revoke");
    }
    revokeSenshu(state, effectiveSide, reasonCode == null ? "MANUAL_REVOKE" : reasonCode, Instant.now(), true);
  }

  private KumiteMatchState requireKumiteState(Match match) {
    if (!isKumite(match.mode)) {
      throw new BadRequestException("Match is not a Kumite match");
    }
    return kumiteStates.findById(match.id)
        .map(state -> syncDurationIfPristine(match, state))
        .orElseGet(() -> kumiteStates.save(createKumiteState(match)));
  }

  private void recomputeKumiteState(Match match, KumiteMatchState state) {
    syncLegacyPenaltyFields(state);
    syncLegacySenshuFlags(state);
    if (hasManualFrozenDecision(state)) {
      return;
    }
    Optional<KumiteDecision> suggestion = suggestKumiteOutcome(match, state);
    if (suggestion.isEmpty()) {
      clearDecision(state);
      return;
    }
    KumiteDecision decision = suggestion.orElseThrow();
    setDecision(state, decision);
    stopTimer(state, Instant.now());
    if (decision.winnerSide() == null && decision.winType() == WinType.HANTEI) {
      match.status = MatchStatus.HANTEI;
    } else if (decision.winnerSide() == null && decision.winType() == WinType.HIKIWAKE) {
      match.status = MatchStatus.HIKIWAKE;
    } else {
      match.status = MatchStatus.RESULT_PENDING_CONFIRMATION;
    }
  }

  private Optional<KumiteDecision> suggestKumiteOutcome(Match match, KumiteMatchState state) {
    ScoreBreakdown breakdown = scoreBreakdown(match.id);
    return kumiteRules.evaluate(
        kumiteRules.profileFor(match),
        matchContext(match),
        new KumiteSnapshot(
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
            breakdown.akaWazaAri(),
            breakdown.aoWazaAri(),
            breakdown.akaIppon(),
            breakdown.aoIppon(),
            currentRemainingMs(state, Instant.now()),
            state.timerRunning
        )
    );
  }

  private KumiteMatchContext matchContext(Match match) {
    if (match.bracket != null && (match.bracket.type == BracketType.ROUND_ROBIN || match.bracket.type == BracketType.POOL)) {
      return new KumiteMatchContext(KumiteMatchFormat.ROUND_ROBIN);
    }
    if (match.mode == CategoryDiscipline.TEAM_KUMITE) {
      return new KumiteMatchContext(isTeamExtraBout(match) ? KumiteMatchFormat.TEAM_EXTRA_BOUT : KumiteMatchFormat.TEAM_REGULAR_BOUT);
    }
    return KumiteMatchContext.individualElimination();
  }

  private boolean isTeamExtraBout(Match match) {
    if (match.teamExtraBout) {
      return true;
    }
    String text = ((match.roundName == null ? "" : match.roundName) + " " + (match.category == null ? "" : match.category.name))
        .toUpperCase();
    return text.contains("EXTRA") || text.contains("TIE-BREAK") || text.contains("TIEBREAK");
  }

  private TeamKumiteAggregate resolveTeamKumiteAggregateIfNeeded(Match match) {
    if (match.mode != CategoryDiscipline.TEAM_KUMITE || match.teamMatchGroupId == null) {
      return null;
    }
    return teamKumiteResolutionService.resolveAfterLockedBout(match);
  }

  private void advanceTeamKumiteAggregateIfResolved(TeamKumiteAggregate aggregate) {
    if (aggregate == null || !aggregate.resolved()) {
      return;
    }
    Match anchor = aggregate.anchorMatch();
    MatchParticipant winnerParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(anchor.id, aggregate.winnerSide())
        .orElse(null);
    if (winnerParticipant == null || winnerParticipant.entry == null) {
      return;
    }
    Entry winnerEntry = winnerParticipant.entry;
    if (anchor.winnerNextMatch != null && anchor.winnerNextSide != null
        && participants.findByMatch_IdAndSideAndDeletedAtIsNull(anchor.winnerNextMatch.id, anchor.winnerNextSide).isEmpty()) {
      advanceWinner(anchor, winnerEntry);
    }
    if (anchor.loserNextMatch != null && anchor.loserNextSide != null
        && participants.findByMatch_IdAndSideAndDeletedAtIsNull(anchor.loserNextMatch.id, anchor.loserNextSide).isEmpty()) {
      advanceLoser(anchor, aggregate.winnerSide());
    }
    if (anchor.roundNumber != null) {
      tournamentPointsService.awardMatchPoints(anchor.id, winnerEntry.id, anchor.roundNumber);
    }
    recordMedalsIfReady(anchor, winnerEntry, aggregate.winnerSide());
  }

  private void publishCreatedExtraBout(TeamKumiteAggregate aggregate) {
    if (aggregate != null && aggregate.createdExtraBout() != null) {
      realtimePublisher.publishMatch(mapper.match(aggregate.createdExtraBout()));
    }
  }

  private void publishNextMatches(Match match, TeamKumiteAggregate aggregate) {
    if (aggregate == null) {
      if (match.winnerNextMatch != null) {
        realtimePublisher.publishMatch(mapper.match(match.winnerNextMatch));
      }
      if (match.loserNextMatch != null) {
        realtimePublisher.publishMatch(mapper.match(match.loserNextMatch));
      }
      return;
    }
    publishTeamAggregateNextMatches(aggregate);
  }

  private void publishTeamAggregateNextMatches(TeamKumiteAggregate aggregate) {
    if (aggregate == null || !aggregate.resolved()) {
      return;
    }
    Match anchor = aggregate.anchorMatch();
    if (anchor.winnerNextMatch != null) {
      realtimePublisher.publishMatch(mapper.match(anchor.winnerNextMatch));
    }
    if (anchor.loserNextMatch != null) {
      realtimePublisher.publishMatch(mapper.match(anchor.loserNextMatch));
    }
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

  private void propagateTenSecondRuleWithdrawal(Match sourceMatch, Side injuredSide) {
    MatchParticipant injuredParticipant = participants
        .findByMatch_IdAndSideAndDeletedAtIsNull(sourceMatch.id, injuredSide)
        .orElse(null);
    if (injuredParticipant == null || injuredParticipant.entry == null) {
      return;
    }
    Entry injuredEntry = injuredParticipant.entry;
    List<Entry> affectedEntries = entries.findByTournament(sourceMatch.tournament.id).stream()
        .filter(entry -> isKumite(entry.category.discipline))
        .filter(entry -> isSameAthleteEntry(injuredEntry, entry))
        .toList();
    if (affectedEntries.isEmpty()) {
      return;
    }
    for (Entry entry : affectedEntries) {
      if (entry.status != EntryStatus.DISQUALIFIED) {
        entry.status = EntryStatus.WITHDRAWN;
      }
    }
    entries.saveAll(affectedEntries);
    resolveFutureMatchesForWithdrawnEntries(sourceMatch, affectedEntries);
  }

  private boolean isSameAthleteEntry(Entry injuredEntry, Entry candidate) {
    if (injuredEntry.id.equals(candidate.id)) {
      return true;
    }
    return injuredEntry.athlete != null
        && candidate.athlete != null
        && injuredEntry.athlete.id.equals(candidate.athlete.id);
  }

  private void resolveFutureMatchesForWithdrawnEntries(Match sourceMatch, List<Entry> affectedEntries) {
    LinkedHashSet<UUID> affectedEntryIds = new LinkedHashSet<>();
    affectedEntries.forEach(entry -> affectedEntryIds.add(entry.id));
    boolean changed;
    do {
      changed = false;
      List<Match> candidateMatches = affectedEntries.stream()
          .flatMap(entry -> participants.findByEntry_IdAndDeletedAtIsNull(entry.id).stream())
          .map(participant -> participant.match)
          .filter(match -> match != null)
          .filter(match -> !sourceMatch.id.equals(match.id))
          .filter(match -> match.tournament.id.equals(sourceMatch.tournament.id))
          .filter(match -> isKumite(match.mode))
          .filter(match -> match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.READY)
          .sorted((left, right) -> {
            int round = Integer.compare(
                left.roundNumber == null ? Integer.MAX_VALUE : left.roundNumber,
                right.roundNumber == null ? Integer.MAX_VALUE : right.roundNumber
            );
            if (round != 0) {
              return round;
            }
            return Integer.compare(
                left.bracketPosition == null ? Integer.MAX_VALUE : left.bracketPosition,
                right.bracketPosition == null ? Integer.MAX_VALUE : right.bracketPosition
            );
          })
          .distinct()
          .toList();
      for (Match match : candidateMatches) {
        if (resolveFutureMatchForWithdrawnEntries(match, affectedEntryIds)) {
          changed = true;
          realtimePublisher.publishMatch(mapper.match(match));
        }
      }
    } while (changed);
  }

  private boolean resolveFutureMatchForWithdrawnEntries(Match match, LinkedHashSet<UUID> affectedEntryIds) {
    MatchParticipant aka = participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, Side.AKA).orElse(null);
    MatchParticipant ao = participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, Side.AO).orElse(null);
    boolean akaWithdrawn = isWithdrawnParticipant(aka, affectedEntryIds);
    boolean aoWithdrawn = isWithdrawnParticipant(ao, affectedEntryIds);
    if (!akaWithdrawn && !aoWithdrawn) {
      return false;
    }
    if (akaWithdrawn && aoWithdrawn) {
      match.status = MatchStatus.CANCELLED;
      match.winType = WinType.TEN_SECOND_RULE;
      return true;
    }
    MatchParticipant winner = akaWithdrawn ? ao : aka;
    if (winner == null || winner.entry == null) {
      match.status = MatchStatus.CANCELLED;
      match.winType = WinType.TEN_SECOND_RULE;
      return true;
    }
    Side winnerSide = akaWithdrawn ? Side.AO : Side.AKA;
    match.winnerEntry = winner.entry;
    match.winnerAthlete = winner.entry.athlete;
    match.winType = WinType.TEN_SECOND_RULE;
    match.status = MatchStatus.LOCKED;
    advanceWinner(match, winner.entry);
    recordMedalsIfReady(match, winner.entry, winnerSide);
    return true;
  }

  private boolean isWithdrawnParticipant(MatchParticipant participant, LinkedHashSet<UUID> affectedEntryIds) {
    return participant != null
        && participant.entry != null
        && (affectedEntryIds.contains(participant.entry.id) || participant.entry.status == EntryStatus.WITHDRAWN);
  }

  private boolean isKumite(CategoryDiscipline discipline) {
    return discipline == CategoryDiscipline.KUMITE || discipline == CategoryDiscipline.TEAM_KUMITE;
  }

  private void validateEventAllowed(Match match, MatchEventRequest request, KumiteMatchState state) {
    if (match.status == MatchStatus.LOCKED || match.status == MatchStatus.CANCELLED || match.status == MatchStatus.COMPLETED) {
      throw new BusinessConflictException("Cannot record events on a " + match.status + " match");
    }
    if (match.status == MatchStatus.HANTEI && request.type() != ScoreEventType.HANTEI_DECISION
        && !(request.type() == ScoreEventType.STATUS_CHANGE && request.status() == MatchStatus.REVIEW)) {
      throw new BusinessConflictException("HANTEI must be resolved by HANTEI_DECISION or REVIEW");
    }
    if (match.status == MatchStatus.HIKIWAKE
        && !(request.type() == ScoreEventType.STATUS_CHANGE && request.status() == MatchStatus.REVIEW)) {
      throw new BusinessConflictException("HIKIWAKE must be confirmed or reopened to REVIEW");
    }
    if (match.status == MatchStatus.RESULT_PENDING_CONFIRMATION
        && !(request.type() == ScoreEventType.STATUS_CHANGE && request.status() == MatchStatus.REVIEW)) {
      throw new BusinessConflictException("Frozen matches must be reopened to REVIEW before new live events");
    }
    if (state != null && state.videoReviewStatus == VideoReviewStatus.REQUESTED && request.type() != ScoreEventType.VIDEO_REVIEW_RESOLVE) {
      throw new BusinessConflictException("Resolve the active video review before other live events");
    }
    if (state != null && state.medicalStatus == MedicalStatus.ACTIVE && request.type() != ScoreEventType.MEDICAL_RESOLVE) {
      throw new BusinessConflictException("Resolve the active medical workflow before other live events");
    }
    if (request.type() == ScoreEventType.STATUS_CHANGE) {
      validateStatusChange(match.status, request.status());
    }
  }

  private void validateStatusChange(MatchStatus current, MatchStatus next) {
    if (next == null) {
      throw new BadRequestException("status is required");
    }
    if (EnumSet.of(MatchStatus.LOCKED, MatchStatus.CANCELLED, MatchStatus.COMPLETED).contains(next)) {
      throw new BusinessConflictException("Use the dedicated flow for status " + next);
    }
    EnumSet<MatchStatus> allowed = switch (current) {
      case SCHEDULED -> EnumSet.of(MatchStatus.READY, MatchStatus.RUNNING, MatchStatus.REVIEW);
      case READY -> EnumSet.of(MatchStatus.RUNNING, MatchStatus.REVIEW);
      case RUNNING -> EnumSet.of(MatchStatus.PAUSED, MatchStatus.REVIEW, MatchStatus.HANTEI, MatchStatus.HIKIWAKE);
      case PAUSED -> EnumSet.of(MatchStatus.RUNNING, MatchStatus.REVIEW, MatchStatus.HANTEI, MatchStatus.HIKIWAKE);
      case REVIEW -> EnumSet.of(MatchStatus.RUNNING, MatchStatus.PAUSED, MatchStatus.HANTEI, MatchStatus.HIKIWAKE);
      case HANTEI -> EnumSet.of(MatchStatus.REVIEW);
      case HIKIWAKE -> EnumSet.of(MatchStatus.REVIEW);
      case RESULT_PENDING_CONFIRMATION -> EnumSet.of(MatchStatus.REVIEW);
      case VOTING -> EnumSet.of(MatchStatus.REVIEW, MatchStatus.PAUSED);
      case COMPLETED, LOCKED, CANCELLED -> EnumSet.noneOf(MatchStatus.class);
    };
    if (!allowed.contains(next)) {
      throw new BusinessConflictException("Cannot change match status from " + current + " to " + next);
    }
  }

  private WinType resolveWinType(Match match, KumiteMatchState state, Side winnerSide, WinType requestedWinType) {
    if (requestedWinType != null && requestedWinType != WinType.MANUAL) {
      return requestedWinType;
    }
    if (!isKumite(match.mode) || state == null) {
      return requestedWinType == null ? WinType.MANUAL : requestedWinType;
    }
    if (state.decisionConfirmable && state.decisionWinnerSide == winnerSide && state.decisionWinType != null) {
      return state.decisionWinType;
    }
    return requestedWinType == null ? WinType.MANUAL : requestedWinType;
  }

  private boolean isHikiwakeConfirmation(Match match, KumiteMatchState state, ConfirmResultRequest request) {
    return match.status == MatchStatus.HIKIWAKE
        && state.decisionFrozen
        && state.decisionConfirmable
        && state.decisionWinnerSide == null
        && state.decisionWinType == WinType.HIKIWAKE
        && (request.winType() == null || request.winType() == WinType.HIKIWAKE || request.winType() == WinType.MANUAL);
  }

  private void confirmHikiwake(Match match, KumiteMatchState state, ConfirmResultRequest request) {
    match.winnerEntry = null;
    match.winnerAthlete = null;
    match.winType = WinType.HIKIWAKE;
    match.status = MatchStatus.LOCKED;
    state.decisionFrozen = false;
    state.decisionConfirmable = false;
    state.videoReviewStatus = VideoReviewStatus.IDLE;
    state.videoReviewActiveSide = null;
    state.medicalStatus = MedicalStatus.IDLE;
    state.medicalInjuredSide = null;
    state.medicalStartedAt = null;
    state.medicalDeadlineAt = null;
    saveResultEvent(match, request);
    clearTatamiCurrentMatchIfLocked(match);
  }

  private KumiteMatchState createKumiteState(Match match) {
    KumiteMatchState state = KumiteMatchState.create();
    state.match = match;
    initializeDuration(match, state);
    return state;
  }

  private KumiteMatchState syncDurationIfPristine(Match match, KumiteMatchState state) {
    int targetDurationMs = resolveDurationMs(match);
    boolean pristine = state.akaScore == 0
        && state.aoScore == 0
        && state.senshuHolder == null
        && state.akaChui == 0
        && state.aoChui == 0
        && !state.akaHansokuChui
        && !state.aoHansokuChui
        && !state.akaHansoku
        && !state.aoHansoku
        && !state.akaShikkaku
        && !state.aoShikkaku
        && !state.akaKiken
        && !state.aoKiken
        && !state.timerRunning
        && state.remainingMs == state.durationMs
        && state.videoReviewStatus == VideoReviewStatus.IDLE
        && state.medicalStatus == MedicalStatus.IDLE;
    if (pristine && state.durationMs != targetDurationMs) {
      state.durationMs = targetDurationMs;
      state.remainingMs = targetDurationMs;
    }
    syncLegacyPenaltyFields(state);
    syncLegacySenshuFlags(state);
    return state;
  }

  private void initializeDuration(Match match, KumiteMatchState state) {
    int durationMs = resolveDurationMs(match);
    state.durationMs = durationMs;
    state.remainingMs = durationMs;
  }

  private int resolveDurationMs(Match match) {
    Integer durationSeconds = match.category == null ? null : match.category.matchDurationSeconds;
    return Math.max(30, durationSeconds == null ? 180 : durationSeconds) * 1000;
  }

  private ScoreBreakdown scoreBreakdown(UUID matchId) {
    int akaWazaAri = 0;
    int aoWazaAri = 0;
    int akaIppon = 0;
    int aoIppon = 0;
    for (MatchScoreEvent event : scoreEvents.findByMatch_IdAndDeletedAtIsNullOrderByOccurredAtAsc(matchId)) {
      if (event.type == ScoreEventType.SCORE_EXCHANGE) {
        ScoreExchange exchange = parsePersistedScoreExchange(event.payloadJson);
        if (exchange.akaPoints() == 2) akaWazaAri += 1;
        if (exchange.aoPoints() == 2) aoWazaAri += 1;
        if (exchange.akaPoints() == 3) akaIppon += 1;
        if (exchange.aoPoints() == 3) aoIppon += 1;
        if (exchange.akaPoints() == -2) akaWazaAri = Math.max(0, akaWazaAri - 1);
        if (exchange.aoPoints() == -2) aoWazaAri = Math.max(0, aoWazaAri - 1);
        if (exchange.akaPoints() == -3) akaIppon = Math.max(0, akaIppon - 1);
        if (exchange.aoPoints() == -3) aoIppon = Math.max(0, aoIppon - 1);
        continue;
      }
      if (event.type != ScoreEventType.SCORE_DELTA || event.side == null || event.points == null) {
        continue;
      }
      switch (event.points) {
        case 2 -> {
          if (event.side == Side.AKA) akaWazaAri += 1;
          else aoWazaAri += 1;
        }
        case 3 -> {
          if (event.side == Side.AKA) akaIppon += 1;
          else aoIppon += 1;
        }
        case -2 -> {
          if (event.side == Side.AKA) akaWazaAri = Math.max(0, akaWazaAri - 1);
          else aoWazaAri = Math.max(0, aoWazaAri - 1);
        }
        case -3 -> {
          if (event.side == Side.AKA) akaIppon = Math.max(0, akaIppon - 1);
          else aoIppon = Math.max(0, aoIppon - 1);
        }
        default -> {
        }
      }
    }
    return new ScoreBreakdown(akaWazaAri, aoWazaAri, akaIppon, aoIppon);
  }

  private ScoreExchange parsePersistedScoreExchange(String payloadJson) {
    try {
      JsonNode root = objectMapper.readTree(payloadJson == null ? "{}" : payloadJson);
      JsonNode deltas = root.path("deltas");
      return new ScoreExchange(
          root.hasNonNull("exchangeId") ? root.path("exchangeId").asText() : null,
          firstInt(root, deltas, "akaPoints", "AKA", "aka"),
          firstInt(root, deltas, "aoPoints", "AO", "ao")
      );
    } catch (JsonProcessingException ex) {
      return new ScoreExchange(null, 0, 0);
    }
  }

  private void applyPenaltyLevel(KumiteMatchState state, Side side, KumitePenaltyLevel level, KumitePenaltyReasonCode reasonCode) {
    if (level == null) {
      throw new BadRequestException("penaltyLevel is required for ladder penalties");
    }
    KumitePenaltyLevel effectiveLevel = enforcePenaltyReasonRules(state, side, level, reasonCode, Instant.now());
    if (side == Side.AKA) {
      state.akaPenaltyLevel = effectiveLevel;
      state.akaPenaltyReasonCode = effectiveLevel == KumitePenaltyLevel.NONE || reasonCode == null ? null : reasonCode.name();
    } else {
      state.aoPenaltyLevel = effectiveLevel;
      state.aoPenaltyReasonCode = effectiveLevel == KumitePenaltyLevel.NONE || reasonCode == null ? null : reasonCode.name();
    }
    syncLegacyPenaltyFields(state);
  }

  private PenaltyCommand normalizePenalty(MatchEventRequest request) {
    if (request.penaltyCategory() != null || request.penaltyLevel() != null || request.penaltyReasonCode() != null) {
      if ("KIKEN".equalsIgnoreCase(request.penaltyReasonCode())) {
        return new PenaltyCommand(request.side(), null, null, null, WinType.KIKEN);
      }
      if ("SHIKKAKU".equalsIgnoreCase(request.penaltyReasonCode())) {
        return new PenaltyCommand(request.side(), null, null, null, WinType.SHIKKAKU);
      }
      KumitePenaltyReasonCode reasonCode = parsePenaltyReasonCode(request.penaltyReasonCode());
      return new PenaltyCommand(
          request.side(),
          request.penaltyCategory(),
          request.penaltyLevel() == null ? KumitePenaltyLevel.NONE : request.penaltyLevel(),
          reasonCode,
          null
      );
    }
    if (request.penaltyCode() == null) {
      throw new BadRequestException("penaltyCode or explicit penalty fields are required for PENALTY");
    }
    String code = request.penaltyCode().trim().toUpperCase();
    return switch (code) {
      case "CHUI" -> new PenaltyCommand(
          request.side(),
          KumitePenaltyCategory.CATEGORY_1,
          legacyChuiLevel(request.points()),
          null,
          null
      );
      case "HANSOKU_CHUI" -> new PenaltyCommand(
          request.side(),
          KumitePenaltyCategory.CATEGORY_1,
          KumitePenaltyLevel.HANSOKU_CHUI,
          null,
          null
      );
      case "HANSOKU" -> new PenaltyCommand(
          request.side(),
          KumitePenaltyCategory.CATEGORY_1,
          KumitePenaltyLevel.HANSOKU,
          null,
          null
      );
      case "KIKEN" -> new PenaltyCommand(request.side(), null, null, null, WinType.KIKEN);
      case "SHIKKAKU" -> new PenaltyCommand(request.side(), null, null, null, WinType.SHIKKAKU);
      default -> throw new BadRequestException("Unsupported penaltyCode: " + request.penaltyCode());
    };
  }

  private KumitePenaltyReasonCode parsePenaltyReasonCode(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      return null;
    }
    String normalized = reasonCode.trim().toUpperCase();
    if ("CATEGORY_1".equals(normalized) || "CATEGORY_2".equals(normalized)) {
      return null;
    }
    try {
      return KumitePenaltyReasonCode.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Unsupported penaltyReasonCode: " + reasonCode);
    }
  }

  private KumitePenaltyLevel legacyChuiLevel(Integer points) {
    int normalized = points == null ? 1 : Math.max(0, Math.min(points, 3));
    return switch (normalized) {
      case 0 -> KumitePenaltyLevel.NONE;
      case 1 -> KumitePenaltyLevel.CHUI_1;
      case 2 -> KumitePenaltyLevel.CHUI_2;
      default -> KumitePenaltyLevel.CHUI_3;
    };
  }

  private void syncLegacyPenaltyFields(KumiteMatchState state) {
    state.akaCategory1Penalty = state.akaPenaltyLevel;
    state.aoCategory1Penalty = state.aoPenaltyLevel;
    state.akaCategory2Penalty = KumitePenaltyLevel.NONE;
    state.aoCategory2Penalty = KumitePenaltyLevel.NONE;
    state.akaHansoku = state.akaPenaltyLevel == KumitePenaltyLevel.HANSOKU;
    state.aoHansoku = state.aoPenaltyLevel == KumitePenaltyLevel.HANSOKU;
    state.akaHansokuChui = state.akaPenaltyLevel.ordinal() >= KumitePenaltyLevel.HANSOKU_CHUI.ordinal();
    state.aoHansokuChui = state.aoPenaltyLevel.ordinal() >= KumitePenaltyLevel.HANSOKU_CHUI.ordinal();
    state.akaChui = legacyChuiValue(state.akaPenaltyLevel);
    state.aoChui = legacyChuiValue(state.aoPenaltyLevel);
  }

  private int legacyChuiValue(KumitePenaltyLevel level) {
    return switch (level) {
      case NONE -> 0;
      case CHUI_1 -> 1;
      case CHUI_2 -> 2;
      case CHUI_3, HANSOKU_CHUI, HANSOKU -> 3;
    };
  }

  private KumitePenaltyLevel enforcePenaltyReasonRules(
      KumiteMatchState state,
      Side side,
      KumitePenaltyLevel level,
      KumitePenaltyReasonCode reasonCode,
      Instant now
  ) {
    if (reasonCode == null || level == KumitePenaltyLevel.NONE) {
      return level;
    }
    int remainingMs = currentRemainingMs(state, now);
    int elapsedMs = Math.max(0, state.durationMs - remainingMs);
    if (reasonCode == KumitePenaltyReasonCode.PASSIVITY && (elapsedMs < 15_000 || remainingMs <= 15_000)) {
      throw new BadRequestException("PASSIVITY cannot be applied in the first or final 15 seconds");
    }
    if (reasonCode == KumitePenaltyReasonCode.AVOIDING_COMBAT && remainingMs <= 15_000) {
      if (state.senshuHolder == side) {
        revokeSenshu(state, side, "AVOIDING_COMBAT", now, true);
      }
      return level.ordinal() < KumitePenaltyLevel.HANSOKU_CHUI.ordinal() ? KumitePenaltyLevel.HANSOKU_CHUI : level;
    }
    return level;
  }

  private void syncLegacySenshuFlags(KumiteMatchState state) {
    state.akaSenshu = state.senshuHolder == Side.AKA;
    state.aoSenshu = state.senshuHolder == Side.AO;
  }

  private void awardSenshu(KumiteMatchState state, Side side, Instant now) {
    state.senshuHolder = side;
    state.senshuAwardedAt = now;
    state.senshuRevoked = false;
    state.senshuRevokedAt = null;
    state.senshuRevocationReasonCode = null;
    syncLegacySenshuFlags(state);
  }

  private void revokeSenshu(KumiteMatchState state, Side side, String reasonCode, Instant now, boolean allowBlock) {
    if (state.senshuHolder != side) {
      return;
    }
    state.senshuHolder = null;
    state.senshuRevoked = true;
    state.senshuRevokedAt = now;
    state.senshuRevocationReasonCode = reasonCode;
    if (allowBlock && currentRemainingMs(state, now) <= 15_000) {
      state.senshuReawardBlocked = true;
    }
    syncLegacySenshuFlags(state);
  }

  private void setDecision(KumiteMatchState state, KumiteDecision decision) {
    state.decisionWinnerSide = decision.winnerSide();
    state.decisionWinType = decision.winType();
    state.decisionReasonCode = decision.reasonCode();
    state.decisionReasonText = decision.reasonText();
    state.decisionFrozen = decision.frozen();
    state.decisionConfirmable = decision.confirmable();
  }

  private void clearDecision(KumiteMatchState state) {
    state.decisionWinnerSide = null;
    state.decisionWinType = null;
    state.decisionReasonCode = null;
    state.decisionReasonText = null;
    state.decisionFrozen = false;
    state.decisionConfirmable = false;
  }

  private boolean hasManualFrozenDecision(KumiteMatchState state) {
    return state.decisionFrozen
        && state.decisionConfirmable
        && (state.decisionWinType == WinType.HANTEI
            || state.decisionWinType == WinType.HIKIWAKE
            || state.decisionWinType == WinType.TEN_SECOND_RULE);
  }

  private boolean videoReviewCardAvailable(KumiteMatchState state, Side side) {
    return side == Side.AKA ? state.akaVideoReviewCardAvailable : state.aoVideoReviewCardAvailable;
  }

  private void consumeVideoReviewCard(KumiteMatchState state, Side side) {
    if (side == Side.AKA) {
      state.akaVideoReviewCardAvailable = false;
    } else {
      state.aoVideoReviewCardAvailable = false;
    }
  }

  private int normalizeResolutionPoints(Integer points) {
    if (points == null || !List.of(1, 2, 3).contains(points)) {
      throw new BadRequestException("resolutionPoints must be 1, 2, or 3");
    }
    return points;
  }

  private String auditPayloadJson(MatchEventRequest request, Side videoReviewRequestingSide) {
    if (request.type() != ScoreEventType.VIDEO_REVIEW_REQUEST
        && request.type() != ScoreEventType.VIDEO_REVIEW_RESOLVE) {
      return request.payloadJson();
    }
    Map<String, Object> audit = new LinkedHashMap<>();
    audit.put("eventType", request.type().name());
    if (request.type() == ScoreEventType.VIDEO_REVIEW_REQUEST) {
      audit.put("requestingSide", request.side());
    } else {
      audit.put("requestingSide", videoReviewRequestingSide);
      audit.put("resolution", request.resolution());
      audit.put("resolutionSide", request.resolutionSide());
      audit.put("resolutionPoints", request.resolutionPoints());
      audit.put("cardConsumed", request.resolution() == VideoReviewResolution.DENIED);
      audit.put("cardKept", request.resolution() != VideoReviewResolution.DENIED);
    }
    audit.put("reasonCode", request.reasonCode());
    audit.put("reasonText", request.reasonText());
    if (request.payloadJson() != null && !request.payloadJson().isBlank()) {
      audit.put("operatorPayload", request.payloadJson());
    }
    try {
      return objectMapper.writeValueAsString(audit);
    } catch (JsonProcessingException ex) {
      return request.payloadJson();
    }
  }

  private void rememberLastLiveStatus(Match match, KumiteMatchState state) {
    if (EnumSet.of(MatchStatus.READY, MatchStatus.RUNNING, MatchStatus.PAUSED, MatchStatus.REVIEW).contains(match.status)) {
      state.lastLiveStatus = match.status;
    }
  }

  private void stopTimer(KumiteMatchState state, Instant now) {
    if (state.timerRunning && state.timerStartedAt != null) {
      state.remainingMs = currentRemainingMs(state, now);
    }
    state.timerRunning = false;
    state.timerStartedAt = null;
  }

  private int currentRemainingMs(KumiteMatchState state, Instant now) {
    if (!state.timerRunning || state.timerStartedAt == null) {
      return state.remainingMs;
    }
    long elapsedMs = Duration.between(state.timerStartedAt, now).toMillis();
    return Math.max(0, state.remainingMs - Math.toIntExact(Math.min(elapsedMs, Integer.MAX_VALUE)));
  }

  private void expireMedicalIfNeeded(Match match, KumiteMatchState state) {
    if (state.medicalStatus != MedicalStatus.ACTIVE || state.medicalDeadlineAt == null || state.medicalInjuredSide == null) {
      return;
    }
    Instant now = Instant.now();
    if (!now.isBefore(state.medicalDeadlineAt)) {
      stopTimer(state, now);
      Side winnerSide = state.medicalInjuredSide == Side.AKA ? Side.AO : Side.AKA;
      state.medicalStatus = MedicalStatus.IDLE;
      state.medicalLastOutcome = MedicalOutcome.UNFIT_TEN_SECOND_RULE;
      state.medicalStartedAt = null;
      state.medicalDeadlineAt = null;
      state.medicalInjuredSide = null;
      setDecision(state, new KumiteDecision(
          winnerSide,
          WinType.TEN_SECOND_RULE,
          "TEN_SECOND_RULE",
          "10 second rule",
          true,
          true
      ));
      match.status = MatchStatus.RESULT_PENDING_CONFIRMATION;
      propagateTenSecondRuleWithdrawal(match, winnerSide == Side.AKA ? Side.AO : Side.AKA);
    }
  }

  private String effectivePenaltyCode(MatchEventRequest request) {
    if (request.penaltyCode() != null) {
      return request.penaltyCode();
    }
    if (request.penaltyReasonCode() != null) {
      return request.penaltyReasonCode();
    }
    if (request.penaltyCategory() != null || request.penaltyLevel() != null) {
      return String.format(
          "%s:%s",
          request.penaltyCategory() == null ? "UNSPECIFIED" : request.penaltyCategory().name(),
          request.penaltyLevel() == null ? "NONE" : request.penaltyLevel().name()
      );
    }
    return null;
  }

  private void clearTatamiCurrentMatchIfLocked(Match match) {
    Tatami tatami = match.tatami;
    if (tatami != null && tatami.currentMatch != null && match.id.equals(tatami.currentMatch.id)) {
      tatami.currentMatch = null;
    }
  }

  private void publishTatamiRollForward(Match match) {
    if (match.tatami == null) {
      return;
    }
    matches.findByTatami_IdAndDeletedAtIsNullAndStatusInOrderByScheduledAtAscMatchNumberAsc(match.tatami.id, TATAMI_LIVE_STATUSES)
        .stream()
        .filter(next -> !next.id.equals(match.id))
        .findFirst()
        .ifPresent(next -> realtimePublisher.publishMatch(mapper.match(next)));
  }

  private record ScoreBreakdown(
      int akaWazaAri,
      int aoWazaAri,
      int akaIppon,
      int aoIppon
  ) {
  }

  private record ScoreExchange(
      String exchangeId,
      int akaPoints,
      int aoPoints
  ) {
  }

  private record PenaltyCommand(
      Side side,
      KumitePenaltyCategory category,
      KumitePenaltyLevel level,
      KumitePenaltyReasonCode reasonCode,
      WinType directOutcome
  ) {
  }
}
