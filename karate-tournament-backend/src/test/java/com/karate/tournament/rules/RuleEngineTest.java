package com.karate.tournament.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteSnapshot;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleEngineTest {
  private final KumiteRuleEngine kumite = new KumiteRuleEngine();
  private final KataRuleEngine kata = new KataRuleEngine();
  private final BracketRuleEngine bracket = new BracketRuleEngine();

  @Test
  void kumiteSuggestsEightPointLead() {
    var suggestion = kumite.suggestWinner(new KumiteSnapshot(
        9, 1, false, false, false, false, false, false, false, false, 90000, true
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().side()).isEqualTo(Side.AKA);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.EIGHT_POINT_LEAD);
  }

  @Test
  void kumiteRequiresHanteiWhenTimeEndsWithoutScoreOrSenshu() {
    var suggestion = kumite.suggestWinner(new KumiteSnapshot(
        0, 0, false, false, false, false, false, false, false, false, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().side()).isNull();
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.HANTEI);
  }

  @Test
  void kataMajorityWorksForFiveJudges() {
    var result = kata.result(Map.of(1, Side.AKA, 2, Side.AO, 3, Side.AKA, 4, Side.AKA, 5, Side.AO), 5);

    assertThat(result.aka()).isEqualTo(3);
    assertThat(result.ao()).isEqualTo(2);
    assertThat(result.winner()).isEqualTo(Side.AKA);
    assertThat(result.complete()).isTrue();
  }

  @Test
  void bracketCalculatesPowerOfTwoAndRoundNames() {
    assertThat(bracket.nextPowerOfTwo(9)).isEqualTo(16);
    assertThat(bracket.roundCount(16)).isEqualTo(4);
    assertThat(bracket.roundName(4, 4)).isEqualTo("Final");
    assertThat(bracket.roundName(3, 4)).isEqualTo("Semifinal");
  }
}
