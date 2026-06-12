package com.karate.tournament.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestExpiryJob {
  private final AttendanceLeaveRequestService leaveRequests;

  public LeaveRequestExpiryJob(AttendanceLeaveRequestService leaveRequests) {
    this.leaveRequests = leaveRequests;
  }

  @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Ho_Chi_Minh")
  public void expireOverdueRequests() {
    leaveRequests.expireOverdueRequests();
  }
}
