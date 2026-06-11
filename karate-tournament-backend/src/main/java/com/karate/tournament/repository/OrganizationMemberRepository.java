package com.karate.tournament.repository;

import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.OrganizationMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
  List<OrganizationMember> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  Optional<OrganizationMember> findByIdAndDeletedAtIsNull(UUID id);

  List<OrganizationMember> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

  long countByOrganization_IdAndStatusAndDeletedAtIsNull(UUID organizationId, ClubMemberStatus status);

  Optional<OrganizationMember> findByOrganization_IdAndPerson_IdAndDeletedAtIsNull(UUID organizationId, UUID personId);

  Optional<OrganizationMember> findByOrganization_IdAndPerson_IdAndStatusAndDeletedAtIsNull(
      UUID organizationId,
      UUID personId,
      ClubMemberStatus status
  );
}
