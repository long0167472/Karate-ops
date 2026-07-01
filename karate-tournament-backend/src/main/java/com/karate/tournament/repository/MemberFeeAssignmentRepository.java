package com.karate.tournament.repository;

import com.karate.tournament.entity.MemberFeeAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberFeeAssignmentRepository extends JpaRepository<MemberFeeAssignment, UUID> {
  List<MemberFeeAssignment> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<MemberFeeAssignment> findByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

  Optional<MemberFeeAssignment> findByIdAndDeletedAtIsNull(UUID id);

  Optional<MemberFeeAssignment> findFirstByMember_IdAndFeeItem_IdAndDeletedAtIsNull(UUID memberId, UUID feeItemId);
}
