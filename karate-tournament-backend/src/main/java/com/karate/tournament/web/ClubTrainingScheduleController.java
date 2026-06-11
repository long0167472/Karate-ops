package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.service.ClubTrainingScheduleService;
import com.karate.tournament.dto.request.ClubTrainingScheduleRequest;
import com.karate.tournament.dto.response.ClubTrainingScheduleResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/training-schedule")
@RequiredArgsConstructor
public class ClubTrainingScheduleController {
  private final ClubTrainingScheduleService schedules;

  @GetMapping
  public ClubTrainingScheduleResponse get(@PathVariable UUID organizationId) {
    return schedules.get(organizationId);
  }

  @PatchMapping
  public ClubTrainingScheduleResponse update(
      @PathVariable UUID organizationId,
      @Valid @RequestBody ClubTrainingScheduleRequest request
  ) {
    return schedules.update(organizationId, request);
  }

  @PostMapping("/ensure-today")
  public int ensureToday(@PathVariable UUID organizationId) {
    return schedules.ensureTodaySession(organizationId);
  }

  @PostMapping("/day-off")
  public int markDayOff(@PathVariable UUID organizationId, @RequestParam LocalDate date) {
    return schedules.markDayOff(organizationId, date);
  }
}
