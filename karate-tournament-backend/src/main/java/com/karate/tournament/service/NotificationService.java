package com.karate.tournament.service;

import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.dto.response.NotificationResponse;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
  NotificationResponse send(UUID userId, String type, String title, String body, String link);

  List<NotificationResponse> list(CurrentActor actor);

  NotificationResponse markRead(CurrentActor actor, UUID notificationId);

  void markAllRead(CurrentActor actor);
}
