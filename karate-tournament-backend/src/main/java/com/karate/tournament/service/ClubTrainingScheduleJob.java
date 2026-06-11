package com.karate.tournament.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClubTrainingScheduleJob {
  private final ClubTrainingScheduleService schedules;

  public ClubTrainingScheduleJob(ClubTrainingScheduleService schedules) {
    this.schedules = schedules;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void ensureTodayOnStartup() {
    schedules.ensureTodaySessions();
  }

  @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
  public void ensureTodayAfterMidnight() {
    schedules.ensureTodaySessions();
  }
}
