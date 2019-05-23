package io.github.avt.env.spreading.topology.raft;

public class VoteRequest {

  private final long term;
  private final String candidate;

  public VoteRequest(long term, String candidate) {
    this.term = term;
    this.candidate = candidate;
  }

  public long term() {
    return term;
  }

  public String candidate() {
    return candidate;
  }
}
