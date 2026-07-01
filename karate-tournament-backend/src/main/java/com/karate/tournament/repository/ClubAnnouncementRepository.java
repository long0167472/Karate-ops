package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubAnnouncement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubAnnouncementRepository extends JpaRepository<ClubAnnouncement, UUID> {
  Optional<ClubAnnouncement> findByIdAndDeletedAtIsNull(UUID id);

  List<ClubAnnouncement> findByOrganization_IdAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(UUID organizationId);

  List<ClubAnnouncement> findByOrganization_IdInAndDeletedAtIsNullOrderByPinnedDescCreatedAtDesc(List<UUID> organizationIds);
}
