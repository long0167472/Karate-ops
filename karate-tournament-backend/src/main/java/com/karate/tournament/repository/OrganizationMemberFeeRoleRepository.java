package com.karate.tournament.repository;

import com.karate.tournament.entity.OrganizationMemberFeeRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberFeeRoleRepository extends JpaRepository<OrganizationMemberFeeRole, UUID> {
  List<OrganizationMemberFeeRole> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId);

  List<OrganizationMemberFeeRole> findByMember_IdAndDeletedAtIsNull(UUID memberId);
}
