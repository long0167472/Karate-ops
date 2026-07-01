package com.karate.tournament.rules;

import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.enums.RulesetPreset;
import com.karate.tournament.entity.enums.RulesetVersion;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class KumiteRuleEngine {
  public KumiteRulesProfile profileFor(Match match) {
    RulesetVersion version = match.category == null ? RulesetVersion.WKF_2026 : match.category.rulesetVersion;
    String snapshot = match.tournament == null ? null : match.tournament.ruleSnapshotJson;
    RulesetPreset preset = match.tournament == null ? RulesetPreset.WKF : match.tournament.rulesetPreset;
    boolean wkf = version == RulesetVersion.WKF_2026
        && (snapshot == null || snapshot.isBlank() || snapshot.toUpperCase().contains("WKF"))
        && preset == RulesetPreset.WKF;
    return wkf
        ? new KumiteRulesProfile("WKF_2026", true, 15_000, 10)
        : new KumiteRulesProfile(String.valueOf(version), true, 15_000, 10);
  }

  public Optional<KumiteDecision> evaluate(KumiteRulesProfile profile, KumiteSnapshot snapshot) {
    return evaluate(profile, KumiteMatchContext.individualElimination(), snapshot);
  }

  public Optional<KumiteDecision> evaluate(
      KumiteRulesProfile profile,
      KumiteMatchContext context,
      KumiteSnapshot snapshot
  ) {
    WinType akaLoss = lossType(snapshot.akaHansoku(), snapshot.akaShikkaku(), snapshot.akaKiken());
    WinType aoLoss = lossType(snapshot.aoHansoku(), snapshot.aoShikkaku(), snapshot.aoKiken());
    if (akaLoss != null && aoLoss == null) {
      return Optional.of(confirmable(Side.AO, akaLoss, akaLoss.name(), reason(akaLoss)));
    }
    if (aoLoss != null && akaLoss == null) {
      return Optional.of(confirmable(Side.AKA, aoLoss, aoLoss.name(), reason(aoLoss)));
    }

    int diff = snapshot.akaScore() - snapshot.aoScore();
    if (Math.abs(diff) >= 8) {
      return Optional.of(confirmable(
          diff > 0 ? Side.AKA : Side.AO,
          WinType.EIGHT_POINT_LEAD,
          "EIGHT_POINT_LEAD",
          "8 point lead"
      ));
    }

    if (snapshot.remainingMs() <= 0 && !snapshot.timerRunning()) {
      if (diff != 0) {
        return Optional.of(confirmable(
            diff > 0 ? Side.AKA : Side.AO,
            WinType.TIME_UP,
            "TIME_UP",
            "Time up"
        ));
      }
      if (snapshot.akaSenshu() && !snapshot.aoSenshu()) {
        return Optional.of(confirmable(Side.AKA, WinType.SENSHU, "SENSHU", "Senshu"));
      }
      if (snapshot.aoSenshu() && !snapshot.akaSenshu()) {
        return Optional.of(confirmable(Side.AO, WinType.SENSHU, "SENSHU", "Senshu"));
      }
      if (profile.useScoreTypeTieBreakers()) {
        int ipponDiff = snapshot.akaIppon() - snapshot.aoIppon();
        if (ipponDiff != 0) {
          return Optional.of(confirmable(
              ipponDiff > 0 ? Side.AKA : Side.AO,
              WinType.TIME_UP,
              "TIME_UP_IPPON",
              "Time up tie-break by ippon"
          ));
        }
        int wazaAriDiff = snapshot.akaWazaAri() - snapshot.aoWazaAri();
        if (wazaAriDiff != 0) {
          return Optional.of(confirmable(
              wazaAriDiff > 0 ? Side.AKA : Side.AO,
              WinType.TIME_UP,
              "TIME_UP_WAZA_ARI",
              "Time up tie-break by waza-ari"
          ));
        }
      }
      if (context.allowsHikiwake()) {
        return Optional.of(new KumiteDecision(null, WinType.HIKIWAKE, "HIKIWAKE", "Hikiwake", true, true));
      }
      return Optional.of(new KumiteDecision(null, WinType.HANTEI, "HANTEI", "Hantei required", true, false));
    }

    return Optional.empty();
  }

  private KumiteDecision confirmable(Side side, WinType winType, String reasonCode, String reasonText) {
    return new KumiteDecision(side, winType, reasonCode, reasonText, true, true);
  }

  private WinType lossType(boolean hansoku, boolean shikkaku, boolean kiken) {
    if (shikkaku) return WinType.SHIKKAKU;
    if (hansoku) return WinType.HANSOKU;
    if (kiken) return WinType.KIKEN;
    return null;
  }

  private String reason(WinType winType) {
    return switch (winType) {
      case KIKEN -> "Opponent withdrew";
      case HANSOKU -> "Opponent lost by hansoku";
      case SHIKKAKU -> "Opponent lost by shikkaku";
      case TEN_SECOND_RULE -> "10 second rule";
      default -> "Decision";
    };
  }

  public record KumiteRulesProfile(
      String name,
      boolean useScoreTypeTieBreakers,
      int senshuProtectedWindowMs,
      int medicalCountdownSeconds
  ) {
  }

  public record KumiteMatchContext(KumiteMatchFormat format) {
    public static KumiteMatchContext individualElimination() {
      return new KumiteMatchContext(KumiteMatchFormat.INDIVIDUAL_ELIMINATION);
    }

    boolean allowsHikiwake() {
      return format == KumiteMatchFormat.ROUND_ROBIN || format == KumiteMatchFormat.TEAM_REGULAR_BOUT;
    }
  }

  public enum KumiteMatchFormat {
    INDIVIDUAL_ELIMINATION,
    ROUND_ROBIN,
    TEAM_REGULAR_BOUT,
    TEAM_EXTRA_BOUT
  }

  public record KumiteDecision(
      Side winnerSide,
      WinType winType,
      String reasonCode,
      String reasonText,
      boolean frozen,
      boolean confirmable
  ) {
  }

  public record KumiteSnapshot(
      int akaScore,
      int aoScore,
      boolean akaSenshu,
      boolean aoSenshu,
      boolean akaHansoku,
      boolean aoHansoku,
      boolean akaShikkaku,
      boolean aoShikkaku,
      boolean akaKiken,
      boolean aoKiken,
      int akaWazaAri,
      int aoWazaAri,
      int akaIppon,
      int aoIppon,
      int remainingMs,
      boolean timerRunning
  ) {
  }
}
