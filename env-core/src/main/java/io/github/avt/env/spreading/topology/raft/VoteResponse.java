package io.github.avt.env.spreading.topology.raft;

public class VoteResponse {

  private long term;
  private boolean voteGranted;

  public VoteResponse(long term, boolean voteGranted) {
    this.term = term;
    this.voteGranted = voteGranted;
  }

  public VoteResponse() {
  }

  public void setTerm(long term) {
    this.term = term;
  }

  public void setVoteGranted(boolean voteGranted) {
    this.voteGranted = voteGranted;
  }

  public long getTerm() {
    return term;
  }

  public boolean isVoteGranted() {
    return voteGranted;
  }

  @Override
  public String toString() {
    return "VoteResponse{" +
      "term=" + term +
      ", voteGranted=" + voteGranted +
      '}';
  }
}
