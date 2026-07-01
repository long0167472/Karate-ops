package com.karate.tournament.web;

import com.karate.tournament.auth.PermissionService;
import com.karate.tournament.dto.response.ExportFileResponse;
import com.karate.tournament.dto.response.MedalTableRow;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.repository.MatchRepository;
import com.karate.tournament.service.DashboardService;
import com.karate.tournament.service.TournamentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/exports")
@RequiredArgsConstructor
public class TournamentExportController {
  private final TournamentService tournaments;
  private final EntryRepository entries;
  private final MatchRepository matches;
  private final DashboardService dashboard;
  private final PermissionService permissions;

  @GetMapping("/entries.csv")
  public ExportFileResponse entries(@PathVariable UUID tournamentId) {
    Tournament tournament = requireTournament(tournamentId);
    List<Entry> rows = entries.findByTournament(tournamentId);
    StringBuilder csv = new StringBuilder("category,delegation,athlete,team,weight_kg,weigh_in_status,status\n");
    rows.forEach(entry -> csv.append(row(
        entry.category.name,
        entry.tournamentParticipant.displayName,
        entry.athlete == null ? "" : entry.athlete.person.displayName,
        entry.teamName == null ? "" : entry.teamName,
        entry.registrationWeightKg == null ? "" : entry.registrationWeightKg.toPlainString(),
        entry.weighInStatus.name(),
        entry.status.name()
    )));
    return csv(tournament, "entries", csv.toString());
  }

  @GetMapping("/schedule.csv")
  public ExportFileResponse schedule(@PathVariable UUID tournamentId) {
    Tournament tournament = requireTournament(tournamentId);
    List<Match> rows = matches.findByTournament_IdAndDeletedAtIsNullOrderByScheduledAtAscMatchNumberAsc(tournamentId);
    StringBuilder csv = new StringBuilder("match_no,tatami,category,round,position,status,mode,scheduled_at\n");
    rows.forEach(match -> csv.append(row(
        String.valueOf(match.matchNumber),
        match.tatami == null ? "" : String.valueOf(match.tatami.tatamiNo),
        match.category.name,
        match.roundName,
        String.valueOf(match.bracketPosition),
        match.status.name(),
        match.mode.name(),
        match.scheduledAt == null ? "" : match.scheduledAt.toString()
    )));
    return csv(tournament, "schedule", csv.toString());
  }

  @GetMapping("/medals.csv")
  public ExportFileResponse medals(@PathVariable UUID tournamentId) {
    Tournament tournament = requireTournament(tournamentId);
    List<MedalTableRow> rows = dashboard.medals(tournamentId);
    StringBuilder csv = new StringBuilder("organization,gold,silver,bronze,total\n");
    rows.forEach(row -> csv.append(row(
        row.organizationName(),
        String.valueOf(row.gold()),
        String.valueOf(row.silver()),
        String.valueOf(row.bronze()),
        String.valueOf(row.total())
    )));
    return csv(tournament, "medals", csv.toString());
  }

  private Tournament requireTournament(UUID tournamentId) {
    Tournament tournament = tournaments.requireTournament(tournamentId);
    permissions.requireViewTournament(tournament);
    return tournament;
  }

  private ExportFileResponse csv(Tournament tournament, String name, String content) {
    String filename = slug(tournament.name) + "-" + name + ".csv";
    return new ExportFileResponse(filename, "text/csv", content);
  }

  private String row(String... cells) {
    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < cells.length; index += 1) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append(escape(cells[index]));
    }
    return builder.append('\n').toString();
  }

  private String escape(String value) {
    String safe = value == null ? "" : value;
    return "\"" + safe.replace("\"", "\"\"") + "\"";
  }

  private String slug(String value) {
    String safe = value == null || value.isBlank() ? "tournament" : value;
    return safe.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }
}
