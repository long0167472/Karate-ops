package com.karate.tournament.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceLeaveRequestExpiryJob {
  private final AttendanceLeaveRequestService leaveRequests;

  @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Ho_Chi_Minh")
  public void expirePendingRequests() {
    leaveRequests.expirePendingRequests();
  }
}
