package com.karate.tournament.service;

import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import java.util.UUID;

public record TeamKumiteAggregate(
    UUID groupId,
    int regularBoutCount,
    int completedRegularBoutCount,
    int akaBoutVictories,
    int aoBoutVictories,
    int akaTotalPoints,
    int aoTotalPoints,
    boolean completeRegularBouts,
    boolean extraBoutRequired,
    Match extraBout,
    Match createdExtraBout,
    Side winnerSide,
    WinType winType,
    String reasonCode,
    Match anchorMatch
) {
  public boolean resolved() {
    return winnerSide != null;
  }
}
