package com.karate.tournament.service.impl;

import lombok.RequiredArgsConstructor;
import com.karate.tournament.dto.response.AthleteRankingResponse;
import com.karate.tournament.dto.response.ClubStandingResponse;
import com.karate.tournament.entity.Entry;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.CategoryResultRepository;
import com.karate.tournament.repository.CategoryResultRepository.MedalTableProjection;
import com.karate.tournament.repository.EntryRepository;
import com.karate.tournament.service.TournamentPointsService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TournamentPointsServiceImpl implements TournamentPointsService {

  private final EntryRepository entries;
  private final CategoryResultRepository categoryResults;

  @Override
  public void awardMatchPoints(UUID matchId, UUID winnerEntryId, int roundNumber) {
    int points = (int) Math.pow(2, roundNumber - 1);
    Entry entry = entries.findByIdAndDeletedAtIsNull(winnerEntryId)
        .orElseThrow(() -> new ResourceNotFoundException("Entry not found: " + winnerEntryId));
    entry.tournamentPoints += points;
    entries.save(entry);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClubStandingResponse> getClubStandings(UUID tournamentId) {
    List<Entry> allEntries = entries.findByTournament(tournamentId);

    // Aggregate points per organization
    Map<UUID, String> orgNames = new HashMap<>();
    Map<UUID, Integer> orgPoints = new HashMap<>();
    for (Entry entry : allEntries) {
      if (entry.tournamentParticipant == null || entry.tournamentParticipant.organization == null) {
        continue;
      }
      UUID orgId = entry.tournamentParticipant.organization.id;
      orgNames.put(orgId, entry.tournamentParticipant.organization.name);
      orgPoints.merge(orgId, entry.tournamentPoints, Integer::sum);
    }

    // Aggregate medals per organization from CategoryResult
    Map<UUID, Long> orgGold = new HashMap<>();
    Map<UUID, Long> orgSilver = new HashMap<>();
    Map<UUID, Long> orgBronze = new HashMap<>();
    List<MedalTableProjection> medalTable = categoryResults.medalTable(tournamentId);
    for (MedalTableProjection row : medalTable) {
      UUID orgId = row.getOrganizationId();
      orgNames.putIfAbsent(orgId, row.getOrganizationName());
      orgGold.put(orgId, row.getGold() == null ? 0L : row.getGold());
      orgSilver.put(orgId, row.getSilver() == null ? 0L : row.getSilver());
      orgBronze.put(orgId, row.getBronze() == null ? 0L : row.getBronze());
    }

    List<ClubStandingResponse> standings = new ArrayList<>();
    for (UUID orgId : orgNames.keySet()) {
      int totalPoints = orgPoints.getOrDefault(orgId, 0);
      int gold = orgGold.getOrDefault(orgId, 0L).intValue();
      int silver = orgSilver.getOrDefault(orgId, 0L).intValue();
      int bronze = orgBronze.getOrDefault(orgId, 0L).intValue();
      standings.add(ClubStandingResponse.of(orgId, orgNames.get(orgId), totalPoints, gold, silver, bronze));
    }

    standings.sort(Comparator
        .comparingInt(ClubStandingResponse::totalPoints).reversed()
        .thenComparingInt(ClubStandingResponse::medalScore).reversed()
        .thenComparing(ClubStandingResponse::organizationName));

    return standings;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AthleteRankingResponse> getAthleteRanking(UUID tournamentId) {
    List<Entry> allEntries = entries.findByTournament(tournamentId);

    // Keep one entry per athlete (highest points if multiple categories)
    Map<UUID, Entry> bestByAthlete = new HashMap<>();
    for (Entry entry : allEntries) {
      if (entry.athlete == null || entry.tournamentPoints <= 0) {
        continue;
      }
      UUID athleteId = entry.athlete.id;
      bestByAthlete.merge(athleteId, entry, (existing, candidate) ->
          candidate.tournamentPoints > existing.tournamentPoints ? candidate : existing
      );
    }

    List<Entry> ranked = new ArrayList<>(bestByAthlete.values());
    ranked.sort(Comparator.comparingInt((Entry e) -> e.tournamentPoints).reversed());

    List<AthleteRankingResponse> result = new ArrayList<>();
    int rank = 1;
    for (int i = 0; i < ranked.size(); i++) {
      Entry entry = ranked.get(i);
      if (i > 0 && ranked.get(i).tournamentPoints < ranked.get(i - 1).tournamentPoints) {
        rank = i + 1;
      }
      UUID orgId = entry.tournamentParticipant != null && entry.tournamentParticipant.organization != null
          ? entry.tournamentParticipant.organization.id : null;
      String orgName = entry.tournamentParticipant != null && entry.tournamentParticipant.organization != null
          ? entry.tournamentParticipant.organization.name : null;
      String athleteName = entry.athlete.person != null ? entry.athlete.person.displayName : null;
      result.add(new AthleteRankingResponse(rank, entry.athlete.id, athleteName, orgId, orgName, entry.tournamentPoints));
    }

    return result;
  }
}
