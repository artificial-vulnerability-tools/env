package io.github.avt.env.spreading.topology.raft;

public class ElectionResult {

  private final int voted;
  private final int total;

  public ElectionResult(int voted, int total) {
    this.voted = voted;
    this.total = total;
  }

  @Override
  public String toString() {
    return String.format("Election result: voted=%d total=%d", voted, total);
  }

  public int voted() {
    return voted;
  }

  public int total() {
    return total;
  }
}
