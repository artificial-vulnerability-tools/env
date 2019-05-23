package io.github.avt.env.spreading.topology.raft;

public class AppendEntriesResponse {

  private long term;
  private boolean success;

  public AppendEntriesResponse(long term, boolean success) {
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

  public AppendEntriesResponse() {
  }

  @Override
  public String toString() {
    return "AppendEntriesResponse{" +
      "term=" + term +
      ", success=" + success +
      '}';
  }
}
