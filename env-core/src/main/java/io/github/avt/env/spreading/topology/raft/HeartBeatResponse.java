package io.github.avt.env.spreading.topology.raft;

public class HeartBeatResponse {

  private long term;
  private boolean success;

  public HeartBeatResponse(long term, boolean success) {
    this.term = term;
    this.success = success;
  }

  public long getTerm() {
    return term;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setTerm(long term) {
    this.term = term;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public HeartBeatResponse() {
  }

  @Override
  public String toString() {
    return "HeartBeatResponse{" +
      "term=" + term +
      ", success=" + success +
      '}';
  }
}
