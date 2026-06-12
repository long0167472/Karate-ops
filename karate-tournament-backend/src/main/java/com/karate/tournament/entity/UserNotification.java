package com.karate.tournament.entity;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_notifications")
public class UserNotification extends BaseEntity {

  public static UserNotification create(AppUser user, String type, String title, String body, String link) {
    UserNotification n = new UserNotification();
    n.appUser = user;
    n.type = type;
    n.title = title;
    n.body = body;
    n.link = link;
    return n;
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "app_user_id")
  public AppUser appUser;

  @Column(nullable = false, length = 80)
  public String type;

  @Column(nullable = false, length = 200)
  public String title;

  @Column(length = 500)
  public String body;

  @Column(length = 500)
  public String link;

  @Column(name = "read_at")
  public Instant readAt;

  public boolean isRead() {
    return readAt != null;
  }

  public void markRead() {
    if (readAt == null) readAt = Instant.now();
  }
}
