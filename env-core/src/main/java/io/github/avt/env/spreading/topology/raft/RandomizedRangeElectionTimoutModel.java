package io.github.avt.env.spreading.topology.raft;

import java.security.SecureRandom;

public class RandomizedRangeElectionTimoutModel implements ElectionTimoutModel {

  private final long fromInclusive;
  private final long toExclusive;
  private final SecureRandom rnd = new SecureRandom();

  public RandomizedRangeElectionTimoutModel(long fromInclusive, long toInclusive) {
    this.fromInclusive = fromInclusive;
    this.toExclusive = toInclusive;
  }

  @Override
  public long generateDelay() {
    long dif = toExclusive - fromInclusive;
    return (rnd.nextLong() % dif) + toExclusive;
  }
}
