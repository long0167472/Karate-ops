package com.karate.tournament.service.impl;

import com.karate.tournament.dto.response.PublicTournamentSummaryResponse;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.TournamentParticipant;
import com.karate.tournament.entity.enums.ParticipantStatus;
import com.karate.tournament.entity.enums.TournamentStatus;
import com.karate.tournament.repository.TournamentParticipantRepository;
import com.karate.tournament.repository.TournamentRepository;
import com.karate.tournament.service.PublicTournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicTournamentServiceImpl implements PublicTournamentService {

  private final TournamentRepository tournaments;
  private final TournamentParticipantRepository participants;

  @Override
  @Transactional(readOnly = true)
  public List<PublicTournamentSummaryResponse> listPublic(String phase) {
    Set<TournamentStatus> statuses = resolveStatuses(phase);

    List<Tournament> all = tournaments.findByDeletedAtIsNullOrderByStartsOnDescCreatedAtDesc();

    return all.stream()
        .filter(t -> statuses.contains(t.status))
        .map(t -> {
          List<TournamentParticipant> allParticipants =
              participants.findByTournament_IdAndDeletedAtIsNullOrderByDisplayNameAsc(t.id);
          int approvedCount = (int) allParticipants.stream()
              .filter(p -> p.status == ParticipantStatus.APPROVED)
              .count();
          boolean registrationOpen = t.status == TournamentStatus.REGISTRATION_OPEN;
          return new PublicTournamentSummaryResponse(
              t.id,
              t.name,
              t.organizerName,
              t.location,
              t.startsOn,
              t.endsOn,
              t.status.name(),
              approvedCount,
              t.phongTraoEnabled,
              t.nangCaoEnabled,
              registrationOpen,
              t.registrationDeadline
          );
        })
        .toList();
  }

  private Set<TournamentStatus> resolveStatuses(String phase) {
    if (phase == null) {
      return Set.of(TournamentStatus.DRAFT, TournamentStatus.REGISTRATION_OPEN, TournamentStatus.REGISTRATION_CLOSED);
    }
    return switch (phase.toUpperCase()) {
      case "ONGOING" -> Set.of(TournamentStatus.RUNNING);
      case "FINISHED" -> Set.of(TournamentStatus.COMPLETED, TournamentStatus.ARCHIVED);
      default -> Set.of(TournamentStatus.DRAFT, TournamentStatus.REGISTRATION_OPEN, TournamentStatus.REGISTRATION_CLOSED);
    };
  }
}
