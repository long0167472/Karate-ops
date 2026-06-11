package com.karate.tournament.rules;

import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class KumiteRuleEngine {
  public Optional<KumiteSuggestion> suggestWinner(KumiteSnapshot snapshot) {
    boolean akaDq = snapshot.akaHansoku() || snapshot.akaShikkaku() || snapshot.akaKiken();
    boolean aoDq = snapshot.aoHansoku() || snapshot.aoShikkaku() || snapshot.aoKiken();
    if (akaDq && !aoDq) {
      return Optional.of(new KumiteSuggestion(Side.AO, WinType.DISQUALIFICATION, "Opponent disqualified"));
    }
    if (aoDq && !akaDq) {
      return Optional.of(new KumiteSuggestion(Side.AKA, WinType.DISQUALIFICATION, "Opponent disqualified"));
    }

    int diff = snapshot.akaScore() - snapshot.aoScore();
    if (Math.abs(diff) >= 8) {
      return Optional.of(new KumiteSuggestion(
          diff > 0 ? Side.AKA : Side.AO,
          WinType.EIGHT_POINT_LEAD,
          "8 point lead"
      ));
    }

    if (snapshot.remainingMs() <= 0 && !snapshot.timerRunning()) {
      if (diff != 0) {
        return Optional.of(new KumiteSuggestion(diff > 0 ? Side.AKA : Side.AO, WinType.TIME_UP, "Time up"));
      }
      if (snapshot.akaSenshu() && !snapshot.aoSenshu()) {
        return Optional.of(new KumiteSuggestion(Side.AKA, WinType.SENSHU, "Senshu"));
      }
      if (snapshot.aoSenshu() && !snapshot.akaSenshu()) {
        return Optional.of(new KumiteSuggestion(Side.AO, WinType.SENSHU, "Senshu"));
      }
      return Optional.of(new KumiteSuggestion(null, WinType.HANTEI, "Hantei required"));
    }

    return Optional.empty();
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
      int remainingMs,
      boolean timerRunning
  ) {
  }

  public record KumiteSuggestion(Side side, WinType winType, String reason) {
  }
}
