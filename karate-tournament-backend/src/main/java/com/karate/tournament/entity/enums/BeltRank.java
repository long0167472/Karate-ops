package com.karate.tournament.entity.enums;

public enum BeltRank {
  WHITE,
  ORANGE,
  BLUE,
  YELLOW,
  GREEN,
  PURPLE,
  BROWN_3,
  BROWN_2,
  BROWN_1,
  BLACK_1,
  BLACK_2,
  BLACK_3,
  BLACK_4,
  BLACK_5;

  public boolean isLowerThan(BeltRank other) {
    return this.ordinal() < other.ordinal();
  }
}
