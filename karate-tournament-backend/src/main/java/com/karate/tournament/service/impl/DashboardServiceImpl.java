package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.*;
import com.karate.tournament.exception.ResourceNotFoundException;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.repository.CategoryRepository;
import com.karate.tournament.repository.CategoryResultRepository;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.repository.TatamiRepository;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.web.ApiMapper;
import com.karate.tournament.dto.response.CategoryDashboardResponse;
import com.karate.tournament.dto.response.DashboardOverviewResponse;
import com.karate.tournament.dto.response.MedalTableRow;
import com.karate.tournament.dto.response.TatamiDashboardRow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
  private final TournamentService tournaments;
  private final CategoryRepository categories;
  private final CategoryResultRepository categoryResults;
  private final EntryRepository entries;
  private final MatchRepository matches;
  private final TatamiRepository tatamis;
  private final TournamentParticipantRepository participants;
  private final PermissionService permissions;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public DashboardOverviewResponse overview(UUID tournamentId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    Map<String, Long> byStatus = new LinkedHashMap<>();
    matches.countByStatus(tournamentId)
        .forEach(row -> byStatus.put(row.getStatus().name(), value(row.getTotal())));
    return new DashboardOverviewResponse(
        tournamentId,
        participants.countByTournament_IdAndDeletedAtIsNull(tournamentId),
        entries.countDistinctAthletesByTournament(tournamentId),
        categories.countByTournament_IdAndDeletedAtIsNull(tournamentId),
        matches.countByTournament_IdAndDeletedAtIsNull(tournamentId),
        matches.countByTournament_IdAndDeletedAtIsNullAndStatusIn(tournamentId, List.of(MatchStatus.COMPLETED, MatchStatus.LOCKED)),
        byStatus
    );
  }

  @Transactional(readOnly = true)
  public List<MedalTableRow> medals(UUID tournamentId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    return categoryResults.medalTable(tournamentId).stream()
        .map(row -> new MedalTableRow(
            row.getOrganizationId(),
            row.getOrganizationName(),
            value(row.getGold()),
            value(row.getSilver()),
            value(row.getBronze()),
            value(row.getTotal())
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TatamiDashboardRow> tatamis(UUID tournamentId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    return tatamis.dashboardRows(
            tournamentId,
            MatchStatus.SCHEDULED,
            List.of(MatchStatus.READY, MatchStatus.RUNNING, MatchStatus.PAUSED, MatchStatus.REVIEW, MatchStatus.HANTEI, MatchStatus.RESULT_PENDING_CONFIRMATION, MatchStatus.VOTING),
            List.of(MatchStatus.COMPLETED, MatchStatus.LOCKED)
        )
        .stream()
        .map(row -> new TatamiDashboardRow(
            row.getTatamiId(),
            row.getTatamiNo(),
            row.getName(),
            value(row.getScheduled()),
            value(row.getRunning()),
            value(row.getCompleted()),
            row.getCurrentMatchId()
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public CategoryDashboardResponse category(UUID tournamentId, UUID categoryId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    Category category = categories.findByIdAndDeletedAtIsNull(categoryId)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    if (!category.tournament.id.equals(tournamentId)) {
      throw new ResourceNotFoundException("Category does not belong to tournament");
    }
    List<Match> categoryMatches = matches.findByCategory_IdAndDeletedAtIsNullOrderByRoundNumberAscBracketPositionAsc(categoryId);
    return new CategoryDashboardResponse(
        category.id,
        category.name,
        entries.countByCategory_IdAndDeletedAtIsNull(categoryId),
        categoryMatches.size(),
        categoryMatches.stream().filter(match -> match.status.name().equals("COMPLETED") || match.status.name().equals("LOCKED")).count(),
        categoryMatches.stream().map(mapper::match).toList()
    );
  }

  private long value(Long value) {
    return value == null ? 0 : value;
  }
}
