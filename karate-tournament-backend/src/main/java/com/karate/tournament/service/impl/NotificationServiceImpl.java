package com.karate.tournament.service.impl;


import lombok.RequiredArgsConstructor;
import com.karate.tournament.auth.CurrentActor;
import com.karate.tournament.dto.response.NotificationResponse;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.UserNotification;
import com.karate.tournament.exception.ResourceNotFoundException;
import com.karate.tournament.repository.AppUserRepository;
import com.karate.tournament.repository.UserNotificationRepository;
import com.karate.tournament.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private static final int MAX_NOTIFICATIONS = 10;

  private final UserNotificationRepository notifications;
  private final AppUserRepository users;

  @Transactional
  public NotificationResponse send(UUID userId, String type, String title, String body, String link) {
    AppUser user = users.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    UserNotification notification = UserNotification.create(user, type, title, body, link);
    notifications.save(notification);

    trimOldest(userId);

    return toResponse(notification);
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> list(CurrentActor actor) {
    return notifications.findByAppUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public NotificationResponse markRead(CurrentActor actor, UUID notificationId) {
    UserNotification notification = notifications
        .findByIdAndAppUser_IdAndDeletedAtIsNull(notificationId, actor.userId())
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    notification.markRead();
    return toResponse(notifications.save(notification));
  }

  @Transactional
  public void markAllRead(CurrentActor actor) {
    notifications.findByAppUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.userId())
        .stream()
        .filter(n -> !n.isRead())
        .forEach(n -> {
          n.markRead();
          notifications.save(n);
        });
  }

  private void trimOldest(UUID userId) {
    long count = notifications.countByAppUser_IdAndReadAtIsNullAndDeletedAtIsNull(userId)
        + notifications.findByAppUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
        .stream().filter(UserNotification::isRead).count();

    // total active (read + unread) for this user
    List<UserNotification> all = notifications.findByAppUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    int excess = all.size() - MAX_NOTIFICATIONS;
    if (excess <= 0) return;

    List<UserNotification> oldest = notifications.findOldestByUserId(userId, PageRequest.of(0, excess));
    oldest.forEach(n -> {
      n.softDelete();
      notifications.save(n);
    });
  }

  private NotificationResponse toResponse(UserNotification n) {
    return new NotificationResponse(
        n.id,
        n.type,
        n.title,
        n.body,
        n.link,
        n.isRead(),
        n.createdAt
    );
  }
}
