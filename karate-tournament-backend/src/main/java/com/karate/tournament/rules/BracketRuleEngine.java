package com.karate.tournament.rules;

import com.karate.tournament.entity.enums.Side;
import org.springframework.stereotype.Component;

@Component
public class BracketRuleEngine {
  public int nextPowerOfTwo(int value) {
    if (value <= 2) {
      return 2;
    }
    int result = 1;
    while (result < value) {
      result <<= 1;
    }
    return result;
  }

  public int roundCount(int bracketSize) {
    int rounds = 0;
    int size = bracketSize;
    while (size > 1) {
      rounds += 1;
      size /= 2;
    }
    return rounds;
  }

  public String roundName(int roundNumber, int totalRounds) {
    if (roundNumber == totalRounds) {
      return "Final";
    }
    if (roundNumber == totalRounds - 1) {
      return "Semifinal";
    }
    if (roundNumber == totalRounds - 2) {
      return "Quarterfinal";
    }
    return "Elimination R" + roundNumber;
  }

  public Side sideForSlot(int zeroBasedSlot) {
    return zeroBasedSlot % 2 == 0 ? Side.AKA : Side.AO;
  }
}
