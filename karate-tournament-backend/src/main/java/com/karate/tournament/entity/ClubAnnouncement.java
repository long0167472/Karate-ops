package com.karate.tournament.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "club_announcements")
public class ClubAnnouncement extends BaseEntity {
  public static ClubAnnouncement create() {
    return new ClubAnnouncement();
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "created_by_user_id")
  public AppUser createdByUser;

  @Column(nullable = false, length = 180)
  public String title;

  @Column(nullable = false, length = 4000)
  public String content;

  @Column(nullable = false)
  public boolean pinned = false;
}
