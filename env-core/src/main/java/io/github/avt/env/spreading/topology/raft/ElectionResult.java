package io.github.avt.env.spreading.topology.raft;

public class ElectionResult {

  private final Integer voted;
  private final Integer total;

  public ElectionResult(int voted, int total) {
    this.voted = voted;
    this.total = total;
  }

  public boolean isQuorum() {
    return voted / total.doubleValue() > 0.5d;
  }

  public int voted() {
    return voted;
  }

  public int total() {
    return total;
  }

  @Override
  public String toString() {
    return String.format("Election result: %s [voted=%d total=%d]", isQuorum() ? "elected" : "not elected", voted(), total());
  }
}
