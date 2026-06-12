package com.karate.tournament.repository;

import com.karate.tournament.entity.UserNotification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

  List<UserNotification> findByAppUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

  long countByAppUser_IdAndReadAtIsNullAndDeletedAtIsNull(UUID userId);

  Optional<UserNotification> findByIdAndAppUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

  @Query("""
      SELECT n FROM UserNotification n
      WHERE n.appUser.id = :userId AND n.deletedAt IS NULL
      ORDER BY n.createdAt ASC
      """)
  List<UserNotification> findOldestByUserId(@Param("userId") UUID userId, PageRequest page);
}
