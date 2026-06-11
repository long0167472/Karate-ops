package com.karate.tournament.repository;

import com.karate.tournament.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
  Optional<AppUser> findByIdAndDeletedAtIsNull(UUID id);

  Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

  Optional<AppUser> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

  boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

  @Query("""
      select u from AppUser u
      where u.deletedAt is null
        and (lower(u.email) = lower(:credential) or lower(u.username) = lower(:credential))
      """)
  Optional<AppUser> findByEmailOrUsername(@Param("credential") String credential);
}
