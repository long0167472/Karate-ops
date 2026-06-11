package com.karate.tournament.rules;

import com.karate.tournament.entity.enums.Side;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KataRuleEngine {
  public KataResult result(Map<Integer, Side> votes, int judgeCount) {
    int normalizedJudgeCount = judgeCount == 7 ? 7 : 5;
    int aka = 0;
    int ao = 0;
    for (int index = 1; index <= normalizedJudgeCount; index += 1) {
      Side side = votes.get(index);
      if (side == Side.AKA) {
        aka += 1;
      }
      if (side == Side.AO) {
        ao += 1;
      }
    }
    int needed = normalizedJudgeCount / 2 + 1;
    Side winner = aka >= needed ? Side.AKA : ao >= needed ? Side.AO : null;
    return new KataResult(aka, ao, needed, winner, aka + ao >= normalizedJudgeCount);
  }

  public record KataResult(int aka, int ao, int needed, Side winner, boolean complete) {
  }
}
