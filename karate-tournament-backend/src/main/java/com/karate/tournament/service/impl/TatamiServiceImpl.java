package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.BadRequestException;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.Tatami;
import com.karate.tournament.entity.enums.TatamiStatus;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.repository.TatamiRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.request.AssignMatchRequest;
import com.karate.tournament.dto.response.MatchResponse;
import com.karate.tournament.dto.request.TatamiCreateRequest;
import com.karate.tournament.dto.response.TatamiResponse;
import com.karate.tournament.dto.request.TatamiUpdateRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TatamiServiceImpl implements TatamiService {
  private static final List<MatchStatus> LIVE_STATUSES = List.copyOf(EnumSet.of(
      MatchStatus.READY,
      MatchStatus.RUNNING,
      MatchStatus.PAUSED,
      MatchStatus.REVIEW,
      MatchStatus.HANTEI,
      MatchStatus.HIKIWAKE,
      MatchStatus.RESULT_PENDING_CONFIRMATION,
      MatchStatus.VOTING
  ));

  private final TatamiRepository tatamis;
  private final MatchRepository matches;
  private final TournamentService tournaments;
  private final PermissionService permissions;
  private final ApiMapper mapper;
  private final RealtimePublisher realtimePublisher;

  @Transactional(readOnly = true)
  public List<TatamiResponse> list(UUID tournamentId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    return tatamis.findByTournament_IdAndDeletedAtIsNullOrderByTatamiNoAsc(tournamentId).stream()
        .map(mapper::tatami)
        .toList();
  }

  @Transactional
  public TatamiResponse create(UUID tournamentId, TatamiCreateRequest request) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireTournamentManage(tournament);
    Tatami tatami = Tatami.create();
    tatami.tournament = tournament;
    tatami.tatamiNo = request.tatamiNo();
    tatami.name = request.name();
    tatami.status = request.status() == null ? TatamiStatus.ACTIVE : request.status();
    return mapper.tatami(tatamis.save(tatami));
  }

  @Transactional
  public TatamiResponse update(UUID id, TatamiUpdateRequest request) {
    Tatami tatami = requireTatami(id);
    permissions.requireTournamentManage(tatami.tournament);
    if (request.tatamiNo() != null) tatami.tatamiNo = request.tatamiNo();
    if (request.name() != null) tatami.name = request.name();
    if (request.status() != null) tatami.status = request.status();
    return mapper.tatami(tatami);
  }

  @Transactional
  public void delete(UUID id) {
    Tatami tatami = requireTatami(id);
    permissions.requireTournamentManage(tatami.tournament);
    tatami.softDelete();
  }

  @Transactional(readOnly = true)
  public MatchResponse currentMatch(UUID tatamiId) {
    Tatami tatami = requireTatami(tatamiId);
    permissions.requireViewTournament(tatami.tournament);
    if (tatami.currentMatch != null && tatami.currentMatch.deletedAt == null && LIVE_STATUSES.contains(tatami.currentMatch.status)) {
      return mapper.match(tatami.currentMatch);
    }
    return matches.findByTatami_IdAndDeletedAtIsNullAndStatusInOrderByScheduledAtAscMatchNumberAsc(tatamiId, LIVE_STATUSES)
        .stream()
        .findFirst()
        .map(mapper::match)
        .orElse(null);
  }

  @Transactional
  public MatchResponse assignMatch(UUID tatamiId, AssignMatchRequest request) {
    Tatami tatami = requireTatami(tatamiId);
    Match match = matches.findByIdAndDeletedAtIsNull(request.matchId())
        .orElseThrow(() -> new ResourceNotFoundException("Match not found: " + request.matchId()));
    if (!match.tournament.id.equals(tatami.tournament.id)) {
      throw new BadRequestException("Match and tatami must belong to the same tournament");
    }
    permissions.requireTatamiOperate(tatami.tournament);
    match.tatami = tatami;
    if (match.status == MatchStatus.SCHEDULED) {
      match.status = MatchStatus.READY;
    }
    tatami.currentMatch = match;
    MatchResponse response = mapper.match(match);
    realtimePublisher.publishMatch(response);
    return response;
  }

  public Tatami requireTatami(UUID id) {
    return tatamis.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tatami not found: " + id));
  }
}
