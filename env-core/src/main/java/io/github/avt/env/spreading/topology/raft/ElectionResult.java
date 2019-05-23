package io.github.avt.env.spreading.topology.raft;

public class ElectionResult {

  private final Long voted;
  private final Long total;

  public ElectionResult(Long voted, Long total) {
    this.voted = voted;
    this.total = total;
  }

  public boolean isQuorum() {
    return voted.doubleValue() / total.doubleValue() > 0.5d;
  }

  public Long voted() {
    return voted;
  }

  public Long total() {
    return total;
  }

  @Override
  public String toString() {
    return String.format("Election result: %s [voted=%d total=%d]", isQuorum() ? "elected" : "not elected", voted(), total());
  }
}
