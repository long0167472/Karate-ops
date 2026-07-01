package com.karate.tournament.repository;

import com.karate.tournament.entity.TournamentJoinRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TournamentJoinRequestRepository extends JpaRepository<TournamentJoinRequest, UUID> {
  Optional<TournamentJoinRequest> findByIdAndDeletedAtIsNull(UUID id);

  /**
   * Locks the request row so concurrent approve/reject decisions serialize; the second
   * transaction sees the already-decided status and fails with a business conflict.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from TournamentJoinRequest r where r.id = :id and r.deletedAt is null")
  Optional<TournamentJoinRequest> findWithLockById(@Param("id") UUID id);

  /**
   * Locks the existing (tournament, member) request during re-submission. Combined with the
   * partial unique index uq_tournament_join_request_active this prevents duplicate requests.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from TournamentJoinRequest r where r.tournament.id = :tournamentId and r.member.id = :memberId and r.deletedAt is null")
  Optional<TournamentJoinRequest> findWithLockByTournamentAndMember(@Param("tournamentId") UUID tournamentId, @Param("memberId") UUID memberId);

  List<TournamentJoinRequest> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<TournamentJoinRequest> findByMember_IdInAndDeletedAtIsNullOrderByCreatedAtDesc(List<UUID> memberIds);
}
