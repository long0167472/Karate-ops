package com.karate.tournament.repository;

import com.karate.tournament.entity.AccountRequest;
import com.karate.tournament.entity.enums.AccountRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, UUID> {
  Optional<AccountRequest> findByIdAndDeletedAtIsNull(UUID id);

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
