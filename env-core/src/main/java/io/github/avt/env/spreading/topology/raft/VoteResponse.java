package io.github.avt.env.spreading.topology.raft;

public class VoteResponse {

  private final long term;
  private final boolean voteGranted;

  public VoteResponse(long term, boolean voteGranted) {
    this.term = term;
    this.voteGranted = voteGranted;
  }

  public long term() {
    return term;
  }

  public boolean voteGranted() {
    return voteGranted;
  }
}
