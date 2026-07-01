package com.karate.tournament.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.karate.tournament.entity.Category;
import com.karate.tournament.entity.Match;
import com.karate.tournament.entity.Tournament;
import com.karate.tournament.entity.enums.Side;
import com.karate.tournament.entity.enums.WinType;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteMatchContext;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteMatchFormat;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteRulesProfile;
import com.karate.tournament.rules.KumiteRuleEngine.KumiteSnapshot;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleEngineTest {
  private final KumiteRuleEngine kumite = new KumiteRuleEngine();
  private final KumiteRulesProfile wkf = new KumiteRulesProfile("WKF_2026", true, 15_000, 10);
  private final KataRuleEngine kata = new KataRuleEngine();
  private final BracketRuleEngine bracket = new BracketRuleEngine();

  @Test
  void wkf2026ProfileUsesScoreTypeTieBreakers() {
    var tournament = Tournament.create();
    var category = Category.create();
    var match = Match.create();
    match.tournament = tournament;
    match.category = category;

    var profile = kumite.profileFor(match);

    assertThat(profile.name()).isEqualTo("WKF_2026");
    assertThat(profile.useScoreTypeTieBreakers()).isTrue();
  }

  @Test
  void kumiteSuggestsEightPointLead() {
    var suggestion = kumite.evaluate(wkf, snapshot(
        9, 1, false, false, false, false, false, false, false, false, 0, 0, 0, 0, 90000, true
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isEqualTo(Side.AKA);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.EIGHT_POINT_LEAD);
  }

  @Test
  void kumiteRequiresHanteiWhenTimeEndsWithoutSenshuOrScoreTypeTieBreak() {
    var suggestion = kumite.evaluate(wkf, KumiteMatchContext.individualElimination(), snapshot(
        0, 0, false, false, false, false, false, false, false, false, 0, 0, 0, 0, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isNull();
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.HANTEI);
  }

  @Test
  void kumiteReturnsHikiwakeForRoundRobinWhenTimeEndsWithoutSenshuOrScoreTypeTieBreak() {
    var suggestion = kumite.evaluate(wkf, new KumiteMatchContext(KumiteMatchFormat.ROUND_ROBIN), snapshot(
        0, 0, false, false, false, false, false, false, false, false, 0, 0, 0, 0, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isNull();
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.HIKIWAKE);
    assertThat(suggestion.orElseThrow().frozen()).isTrue();
    assertThat(suggestion.orElseThrow().confirmable()).isTrue();
  }

  @Test
  void kumiteReturnsHanteiForTeamExtraBoutWhenTimeEndsWithoutSenshuOrScoreTypeTieBreak() {
    var suggestion = kumite.evaluate(wkf, new KumiteMatchContext(KumiteMatchFormat.TEAM_EXTRA_BOUT), snapshot(
        0, 0, false, false, false, false, false, false, false, false, 0, 0, 0, 0, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isNull();
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.HANTEI);
  }

  @Test
  void kumiteGivesSenshuWhenTimeEndsLevelAndSenshuExists() {
    var suggestion = kumite.evaluate(wkf, snapshot(
        6, 6, true, false, false, false, false, false, false, false, 1, 0, 1, 0, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isEqualTo(Side.AKA);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.SENSHU);
  }

  @Test
  void kumiteBreaksWkf2026TimeUpTieByIpponBeforeHantei() {
    var suggestion = kumite.evaluate(wkf, snapshot(
        6, 6, false, false, false, false, false, false, false, false, 0, 2, 2, 1, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isEqualTo(Side.AKA);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.TIME_UP);
    assertThat(suggestion.orElseThrow().reasonCode()).isEqualTo("TIME_UP_IPPON");
  }

  @Test
  void kumiteBreaksWkf2026TimeUpTieByWazaAriAfterEqualIppon() {
    var suggestion = kumite.evaluate(wkf, snapshot(
        6, 6, false, false, false, false, false, false, false, false, 1, 2, 1, 1, 0, false
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isEqualTo(Side.AO);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.TIME_UP);
    assertThat(suggestion.orElseThrow().reasonCode()).isEqualTo("TIME_UP_WAZA_ARI");
  }

  @Test
  void kumiteReturnsKikenInsteadOfGenericDisqualification() {
    var suggestion = kumite.evaluate(wkf, snapshot(
        0, 0, false, false, false, false, false, false, false, true, 0, 0, 0, 0, 60000, true
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isEqualTo(Side.AKA);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.KIKEN);
  }

  @Test
  void kumiteReturnsHansokuInsteadOfGenericDisqualification() {
    var suggestion = kumite.evaluate(wkf, snapshot(
        0, 0, false, false, false, true, false, false, false, false, 0, 0, 0, 0, 60000, true
    ));

    assertThat(suggestion).isPresent();
    assertThat(suggestion.orElseThrow().winnerSide()).isEqualTo(Side.AKA);
    assertThat(suggestion.orElseThrow().winType()).isEqualTo(WinType.HANSOKU);
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

  private KumiteSnapshot snapshot(
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
    return new KumiteSnapshot(
        akaScore,
        aoScore,
        akaSenshu,
        aoSenshu,
        akaHansoku,
        aoHansoku,
        akaShikkaku,
        aoShikkaku,
        akaKiken,
        aoKiken,
        akaWazaAri,
        aoWazaAri,
        akaIppon,
        aoIppon,
        remainingMs,
        timerRunning
    );
  }
}
