package io.github.avt.env.spreading.topology.raft;

public class VoteRequest {

  private long term;
  private String candidate;

  public VoteRequest(long term, String candidate) {
    this.term = term;
    this.candidate = candidate;
  }

  public VoteRequest() {
  }

  public void setTerm(long term) {
    this.term = term;
  }

  public void setCandidate(String candidate) {
    this.candidate = candidate;
  }

  public long getTerm() {
    return term;
  }

  public String getCandidate() {
    return candidate;
  }

  @Override
  public String toString() {
    return "VoteRequest{" +
      "term=" + term +
      ", candidate='" + candidate + '\'' +
      '}';
  }
}
