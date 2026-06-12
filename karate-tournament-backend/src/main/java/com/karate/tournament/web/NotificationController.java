package com.karate.tournament.web;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.auth.CurrentActorProvider;
import com.karate.tournament.dto.response.NotificationResponse;
import com.karate.tournament.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final CurrentActorProvider currentActorProvider;

  @GetMapping
  public List<NotificationResponse> list() {
    return notificationService.list(currentActorProvider.currentActor());
  }

  @PostMapping("/{id}/read")
  public NotificationResponse markRead(@PathVariable UUID id) {
    return notificationService.markRead(currentActorProvider.currentActor(), id);
  }

  @PostMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAllRead() {
    notificationService.markAllRead(currentActorProvider.currentActor());
  }
}
