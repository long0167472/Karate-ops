package com.karate.tournament.service;

import com.karate.tournament.dto.response.DashboardOverviewResponse;
import com.karate.tournament.dto.response.MatchResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RealtimePublisher {
  private final SimpMessagingTemplate messagingTemplate;
  private final DashboardService dashboardService;

  public RealtimePublisher(SimpMessagingTemplate messagingTemplate, DashboardService dashboardService) {
    this.messagingTemplate = messagingTemplate;
    this.dashboardService = dashboardService;
  }

  public void publishMatch(MatchResponse response) {
    if (response.tatamiId() != null) {
      messagingTemplate.convertAndSend("/topic/tatamis/" + response.tatamiId(), response);
    }
    DashboardOverviewResponse overview = dashboardService.overview(response.tournamentId());
    messagingTemplate.convertAndSend("/topic/tournaments/" + response.tournamentId() + "/dashboard", overview);
  }
}
