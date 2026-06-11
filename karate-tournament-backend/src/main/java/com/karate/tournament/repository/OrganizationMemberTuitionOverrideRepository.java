package com.karate.tournament.repository;

import com.karate.tournament.entity.OrganizationMemberTuitionOverride;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberTuitionOverrideRepository extends JpaRepository<OrganizationMemberTuitionOverride, UUID> {
  List<OrganizationMemberTuitionOverride> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId);

  List<OrganizationMemberTuitionOverride> findByMember_IdAndDeletedAtIsNull(UUID memberId);

  Optional<OrganizationMemberTuitionOverride> findFirstByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);
}
