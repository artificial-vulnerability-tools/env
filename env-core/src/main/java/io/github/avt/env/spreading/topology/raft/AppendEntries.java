package io.github.avt.env.spreading.topology.raft;

public class AppendEntries {

  private long term;
  private String leaderId;

  public AppendEntries(long term, String leaderId) {
    this.term = term;
    this.leaderId = leaderId;
  }

  public AppendEntries() {
  }

  public long getTerm() {
    return term;
  }

  public String getLeaderId() {
    return leaderId;
  }

  @Override
  public String toString() {
    return "AppendEntries{" +
      "term=" + term +
      ", leaderId='" + leaderId + '\'' +
      '}';
  }
}
