package com.karate.tournament.repository;

import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.enums.Side;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {
  List<MatchParticipant> findByMatch_IdAndDeletedAtIsNullOrderBySideAsc(UUID matchId);

  Optional<MatchParticipant> findByMatch_IdAndSideAndDeletedAtIsNull(UUID matchId, Side side);

  List<MatchParticipant> findByEntry_IdAndDeletedAtIsNull(UUID entryId);
}
