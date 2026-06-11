package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubFeeItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubFeeItemRepository extends JpaRepository<ClubFeeItem, UUID> {
  List<ClubFeeItem> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  Optional<ClubFeeItem> findByIdAndDeletedAtIsNull(UUID id);
}
