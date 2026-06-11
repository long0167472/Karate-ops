package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubFeeItemRoleAmount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubFeeItemRoleAmountRepository extends JpaRepository<ClubFeeItemRoleAmount, UUID> {
  List<ClubFeeItemRoleAmount> findByFeeItem_IdAndDeletedAtIsNull(UUID feeItemId);
}
