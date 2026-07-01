package com.karate.tournament.service;

import com.karate.tournament.entity.KumiteMatchState;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.MatchParticipant;
import com.karate.tournament.entity.enums.CategoryDiscipline;
import com.karate.tournament.entity.enums.MatchStatus;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.repository.KumiteMatchStateRepository;
import com.karate.tournament.repository.MatchParticipantRepository;
import com.karate.tournament.repository.MatchRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamKumiteResolutionService {
  private final MatchRepository matches;
  private final MatchParticipantRepository participants;
  private final KumiteMatchStateRepository kumiteStates;

  public TeamKumiteAggregate resolveAfterLockedBout(Match sourceMatch) {
    if (!isGroupedTeamKumite(sourceMatch)) {
      return null;
    }
    List<Match> groupMatches = groupMatches(sourceMatch);
    TeamKumiteAggregate aggregate = aggregate(sourceMatch, groupMatches);
    if (aggregate.extraBoutRequired() && aggregate.extraBout() == null) {
      Match extraBout = createExtraBout(aggregate.anchorMatch(), groupMatches);
      groupMatches = groupMatches(sourceMatch);
      return aggregate(sourceMatch, groupMatches, extraBout);
    }
    return aggregate;
  }

  public TeamKumiteAggregate aggregateFor(Match sourceMatch) {
    if (!isGroupedTeamKumite(sourceMatch)) {
      return null;
    }
    return aggregate(sourceMatch, groupMatches(sourceMatch));
  }

  private boolean isGroupedTeamKumite(Match match) {
    return match != null
        && match.mode == CategoryDiscipline.TEAM_KUMITE
        && match.teamMatchGroupId != null;
  }

  private List<Match> groupMatches(Match sourceMatch) {
    return matches.findByTeamMatchGroupIdAndDeletedAtIsNullOrderByTeamBoutOrderAscMatchNumberAsc(sourceMatch.teamMatchGroupId);
  }

  private TeamKumiteAggregate aggregate(Match sourceMatch, List<Match> groupMatches) {
    return aggregate(sourceMatch, groupMatches, null);
  }

  private TeamKumiteAggregate aggregate(Match sourceMatch, List<Match> groupMatches, Match createdExtraBout) {
    List<Match> regularBouts = groupMatches.stream()
        .filter(match -> !match.teamExtraBout)
        .toList();
    Match extraBout = groupMatches.stream()
        .filter(match -> match.teamExtraBout)
        .findFirst()
        .orElse(createdExtraBout);
    Match anchor = groupMatches.stream()
        .filter(match -> !match.teamExtraBout)
        .min(Comparator.comparing((Match match) -> nullableOrder(match.teamBoutOrder))
            .thenComparing(match -> nullableOrder(match.matchNumber)))
        .orElse(sourceMatch);

    int akaVictories = 0;
    int aoVictories = 0;
    int akaPoints = 0;
    int aoPoints = 0;
    int completedRegular = 0;
    for (Match bout : regularBouts) {
      if (!isTerminal(bout)) {
        continue;
      }
      completedRegular += 1;
      Optional<KumiteMatchState> state = kumiteStates.findById(bout.id);
      akaPoints += state.map(value -> value.akaScore).orElse(0);
      aoPoints += state.map(value -> value.aoScore).orElse(0);
      Side winnerSide = winnerSide(bout);
      if (winnerSide == Side.AKA) {
        akaVictories += 1;
      } else if (winnerSide == Side.AO) {
        aoVictories += 1;
      }
    }

    boolean completeRegular = !regularBouts.isEmpty() && completedRegular == regularBouts.size();
    if (!completeRegular) {
      return new TeamKumiteAggregate(sourceMatch.teamMatchGroupId, regularBouts.size(), completedRegular,
          akaVictories, aoVictories, akaPoints, aoPoints, false, false, extraBout, createdExtraBout,
          null, null, null, anchor);
    }
    if (akaVictories != aoVictories) {
      Side side = akaVictories > aoVictories ? Side.AKA : Side.AO;
      return resolved(sourceMatch, regularBouts.size(), completedRegular, akaVictories, aoVictories, akaPoints, aoPoints,
          extraBout, createdExtraBout, side, WinType.POINTS, "TEAM_BOUT_VICTORIES", anchor);
    }
    if (akaPoints != aoPoints) {
      Side side = akaPoints > aoPoints ? Side.AKA : Side.AO;
      return resolved(sourceMatch, regularBouts.size(), completedRegular, akaVictories, aoVictories, akaPoints, aoPoints,
          extraBout, createdExtraBout, side, WinType.POINTS, "TEAM_TOTAL_POINTS", anchor);
    }
    if (extraBout != null && isTerminal(extraBout)) {
      Side extraWinner = winnerSide(extraBout);
      if (extraWinner != null) {
        return resolved(sourceMatch, regularBouts.size(), completedRegular, akaVictories, aoVictories, akaPoints, aoPoints,
            extraBout, createdExtraBout, extraWinner, extraBout.winType, "TEAM_EXTRA_BOUT", anchor);
      }
    }
    return new TeamKumiteAggregate(sourceMatch.teamMatchGroupId, regularBouts.size(), completedRegular,
        akaVictories, aoVictories, akaPoints, aoPoints, true, true, extraBout, createdExtraBout,
        null, null, "TEAM_EXTRA_BOUT_REQUIRED", anchor);
  }

  private TeamKumiteAggregate resolved(
      Match sourceMatch,
      int regularBoutCount,
      int completedRegularBoutCount,
      int akaVictories,
      int aoVictories,
      int akaPoints,
      int aoPoints,
      Match extraBout,
      Match createdExtraBout,
      Side winnerSide,
      WinType winType,
      String reasonCode,
      Match anchor
  ) {
    return new TeamKumiteAggregate(sourceMatch.teamMatchGroupId, regularBoutCount, completedRegularBoutCount,
        akaVictories, aoVictories, akaPoints, aoPoints, true, false, extraBout, createdExtraBout,
        winnerSide, winType, reasonCode, anchor);
  }

  private Match createExtraBout(Match anchor, List<Match> groupMatches) {
    Match extraBout = Match.create();
    extraBout.tournament = anchor.tournament;
    extraBout.category = anchor.category;
    extraBout.bracket = anchor.bracket;
    extraBout.tatami = anchor.tatami;
    extraBout.matchNumber = nextMatchNumber(groupMatches);
    extraBout.roundName = extraRoundName(anchor);
    extraBout.roundNumber = anchor.roundNumber;
    extraBout.bracketPosition = anchor.bracketPosition;
    extraBout.status = MatchStatus.READY;
    extraBout.mode = CategoryDiscipline.TEAM_KUMITE;
    extraBout.winnerNextMatch = anchor.winnerNextMatch;
    extraBout.winnerNextSide = anchor.winnerNextSide;
    extraBout.loserNextMatch = anchor.loserNextMatch;
    extraBout.loserNextSide = anchor.loserNextSide;
    extraBout.teamMatchGroupId = anchor.teamMatchGroupId;
    extraBout.teamBoutOrder = nextBoutOrder(groupMatches);
    extraBout.teamExtraBout = true;
    matches.save(extraBout);
    copyParticipant(anchor, extraBout, Side.AKA);
    copyParticipant(anchor, extraBout, Side.AO);
    KumiteMatchState state = KumiteMatchState.create();
    state.match = extraBout;
    initializeDuration(anchor, state);
    kumiteStates.save(state);
    return extraBout;
  }

  private void initializeDuration(Match anchor, KumiteMatchState state) {
    Integer durationSeconds = anchor.category == null ? null : anchor.category.matchDurationSeconds;
    int durationMs = Math.max(30, durationSeconds == null ? 180 : durationSeconds) * 1000;
    state.durationMs = durationMs;
    state.remainingMs = durationMs;
  }

  private void copyParticipant(Match anchor, Match extraBout, Side side) {
    participants.findByMatch_IdAndSideAndDeletedAtIsNull(anchor.id, side)
        .ifPresent(source -> {
          MatchParticipant target = MatchParticipant.create();
          target.match = extraBout;
          target.side = side;
          target.entry = source.entry;
          participants.save(target);
        });
  }

  private Side winnerSide(Match match) {
    if (match.winnerEntry == null) {
      return null;
    }
    return participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, Side.AKA)
        .filter(participant -> participant.entry != null && participant.entry.id.equals(match.winnerEntry.id))
        .map(participant -> Side.AKA)
        .or(() -> participants.findByMatch_IdAndSideAndDeletedAtIsNull(match.id, Side.AO)
            .filter(participant -> participant.entry != null && participant.entry.id.equals(match.winnerEntry.id))
            .map(participant -> Side.AO))
        .orElse(null);
  }

  private boolean isTerminal(Match match) {
    return match.status == MatchStatus.LOCKED || match.status == MatchStatus.COMPLETED;
  }

  private int nextMatchNumber(List<Match> groupMatches) {
    return groupMatches.stream()
        .map(match -> match.matchNumber)
        .filter(value -> value != null)
        .max(Integer::compareTo)
        .orElse(0) + 1;
  }

  private int nextBoutOrder(List<Match> groupMatches) {
    return groupMatches.stream()
        .map(match -> match.teamBoutOrder)
        .filter(value -> value != null)
        .max(Integer::compareTo)
        .orElse(0) + 1;
  }

  private Integer nullableOrder(Integer value) {
    return value == null ? Integer.MAX_VALUE : value;
  }

  private String extraRoundName(Match anchor) {
    String roundName = anchor.roundName == null || anchor.roundName.isBlank() ? "Team Kumite" : anchor.roundName;
    return roundName.toUpperCase().contains("EXTRA") ? roundName : roundName + " Extra Bout";
  }
}
