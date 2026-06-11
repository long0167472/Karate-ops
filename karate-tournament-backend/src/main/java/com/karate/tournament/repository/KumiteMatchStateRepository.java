package com.karate.tournament.repository;

import com.karate.tournament.entity.KumiteMatchState;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KumiteMatchStateRepository extends JpaRepository<KumiteMatchState, UUID> {
}
