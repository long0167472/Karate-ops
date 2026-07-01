package com.karate.tournament.repository;

import com.karate.tournament.entity.AttendanceSession;
import com.karate.tournament.entity.enums.AttendanceSessionSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {
  List<AttendanceSession> findByOrganization_IdAndDeletedAtIsNullOrderByScheduledAtDescCreatedAtDesc(UUID organizationId);

  List<AttendanceSession> findByOrganization_IdAndScheduledDateBetweenAndDeletedAtIsNullOrderByScheduledAtAscCreatedAtAsc(UUID organizationId, LocalDate from, LocalDate to);

  List<AttendanceSession> findByTrainingSchedule_IdAndSourceAndScheduledDateBetweenAndDeletedAtIsNull(UUID scheduleId, AttendanceSessionSource source, LocalDate from, LocalDate to);

  Optional<AttendanceSession> findByIdAndDeletedAtIsNull(UUID id);

  @Query("""
      select s.organization.id as organizationId, count(s) as total
      from AttendanceSession s
      where s.organization.id in :organizationIds
        and s.deletedAt is null
      group by s.organization.id
      """)
  List<OrganizationCountProjection> countByOrganizationIds(@Param("organizationIds") List<UUID> organizationIds);

  interface OrganizationCountProjection {
    UUID getOrganizationId();

    Long getTotal();
  }
}
