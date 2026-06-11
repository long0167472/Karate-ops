package com.karate.tournament.repository;

import com.karate.tournament.entity.MatchAuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAuditEventRepository extends JpaRepository<MatchAuditEvent, UUID> {
}
