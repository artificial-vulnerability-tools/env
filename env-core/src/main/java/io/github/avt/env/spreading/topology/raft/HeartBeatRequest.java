package io.github.avt.env.spreading.topology.raft;

public class HeartBeatRequest {

  private long term;
  private String leaderId;

  public HeartBeatRequest(long term, String leaderId) {
    this.term = term;
    this.leaderId = leaderId;
  }

  public HeartBeatRequest() {
  }

  public long getTerm() {
    return term;
  }

  public String getLeaderId() {
    return leaderId;
  }

  @Override
  public String toString() {
    return "HeartBeatRequest{" +
      "term=" + term +
      ", leaderId='" + leaderId + '\'' +
      '}';
  }
}
