package com.karate.tournament.repository;

import com.karate.tournament.entity.TournamentParticipant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, UUID> {
  List<TournamentParticipant> findByTournament_IdAndDeletedAtIsNullOrderByDisplayNameAsc(UUID tournamentId);

  Optional<TournamentParticipant> findByIdAndDeletedAtIsNull(UUID id);

  /** Locks the participant row so concurrent status transitions serialize. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from TournamentParticipant p where p.id = :id and p.deletedAt is null")
  Optional<TournamentParticipant> findWithLockById(@Param("id") UUID id);

  Optional<TournamentParticipant> findByTournament_IdAndOrganization_IdAndDeletedAtIsNull(UUID tournamentId, UUID organizationId);

  long countByTournament_IdAndDeletedAtIsNull(UUID tournamentId);
}
