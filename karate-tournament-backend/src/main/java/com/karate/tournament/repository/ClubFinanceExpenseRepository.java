package com.karate.tournament.repository;

import com.karate.tournament.entity.ClubFinanceExpense;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubFinanceExpenseRepository extends JpaRepository<ClubFinanceExpense, UUID> {
  List<ClubFinanceExpense> findByOrganization_IdAndDeletedAtIsNullOrderByExpenseDateDescCreatedAtDesc(UUID organizationId);

  Optional<ClubFinanceExpense> findByIdAndDeletedAtIsNull(UUID id);
}
