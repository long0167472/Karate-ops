package com.karate.tournament.repository;

import com.karate.tournament.entity.enums.ClubMemberStatus;
import com.karate.tournament.entity.OrganizationMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
  List<OrganizationMember> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  Optional<OrganizationMember> findByIdAndDeletedAtIsNull(UUID id);

  List<OrganizationMember> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

  long countByOrganization_IdAndStatusAndDeletedAtIsNull(UUID organizationId, ClubMemberStatus status);

  @Query("""
      select m.organization.id as organizationId, count(m) as total
      from OrganizationMember m
      where m.organization.id in :organizationIds
        and m.status = :status
        and m.deletedAt is null
      group by m.organization.id
      """)
  List<OrganizationCountProjection> countByOrganizationIdsAndStatus(
      @Param("organizationIds") List<UUID> organizationIds,
      @Param("status") ClubMemberStatus status
  );

  Optional<OrganizationMember> findByOrganization_IdAndPerson_IdAndDeletedAtIsNull(UUID organizationId, UUID personId);

  Optional<OrganizationMember> findByOrganization_IdAndPerson_IdAndStatusAndDeletedAtIsNull(
      UUID organizationId,
      UUID personId,
      ClubMemberStatus status
  );

  interface OrganizationCountProjection {
    UUID getOrganizationId();

    Long getTotal();
  }
}
