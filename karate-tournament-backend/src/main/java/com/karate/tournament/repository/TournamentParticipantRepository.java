package com.karate.tournament.repository;

import com.karate.tournament.entity.TournamentParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, UUID> {
  List<TournamentParticipant> findByTournament_IdAndDeletedAtIsNullOrderByDisplayNameAsc(UUID tournamentId);

  Optional<TournamentParticipant> findByIdAndDeletedAtIsNull(UUID id);

  Optional<TournamentParticipant> findByTournament_IdAndOrganization_IdAndDeletedAtIsNull(UUID tournamentId, UUID organizationId);

  long countByTournament_IdAndDeletedAtIsNull(UUID tournamentId);
}
