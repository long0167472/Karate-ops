package com.karate.tournament.repository;

import com.karate.tournament.entity.AccountRequest;
import com.karate.tournament.entity.enums.AccountRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, UUID> {
  Optional<AccountRequest> findByIdAndDeletedAtIsNull(UUID id);

  /**
   * Locks the account request row so two managers deciding at once cannot both pass the
   * PENDING status check and provision the account twice.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from AccountRequest r where r.id = :id and r.deletedAt is null")
  Optional<AccountRequest> findWithLockById(@Param("id") UUID id);

  Optional<AccountRequest> findByOrganization_IdAndEmailIgnoreCaseAndStatusAndDeletedAtIsNull(
      UUID organizationId,
      String email,
      AccountRequestStatus status
  );

  List<AccountRequest> findByOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

  List<AccountRequest> findByOrganization_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID organizationId,
      AccountRequestStatus status
  );
}
