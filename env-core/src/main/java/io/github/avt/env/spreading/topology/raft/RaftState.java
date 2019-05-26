package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;

public abstract class RaftState {

  private long currentTerm = 0;
  private InfectedHost votedFor = null;

  public abstract String stateName();

  public RaftState(RaftState state) {
    this.currentTerm = state.currentTerm;
    this.votedFor = state.votedFor;
  }

  public RaftState(long currentTerm, InfectedHost votedFor) {
    this.currentTerm = currentTerm;
    this.votedFor = votedFor;
  }

  public long getCurrentTerm() {
    return currentTerm;
  }

  public void setCurrentTerm(long currentTerm) {
    this.currentTerm = currentTerm;
  }

  public InfectedHost getVotedFor() {
    return votedFor;
  }

  public void setVotedFor(InfectedHost votedFor) {
    this.votedFor = votedFor;
  }

  @Override
  public String toString() {
    return "state=" + stateName() +
      ", currentTerm=" + currentTerm + ", " +
      "votedFor=" + votedFor.toString();

  }
}
