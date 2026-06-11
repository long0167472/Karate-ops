package com.karate.tournament.service;

import com.karate.tournament.dto.request.ClubTrainingScheduleRequest;
import com.karate.tournament.dto.response.ClubTrainingScheduleResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface ClubTrainingScheduleService {
  ClubTrainingScheduleResponse get(UUID organizationId);
  ClubTrainingScheduleResponse update(UUID organizationId, ClubTrainingScheduleRequest request);
  int ensureTodaySessions();
  int ensureTodaySession(UUID organizationId);
  int markDayOff(UUID organizationId, LocalDate date);
}
