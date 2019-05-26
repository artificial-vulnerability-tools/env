package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;

public class RaftState {

  private long currentTerm = 0;
  private InfectedHost votedFor = null;
  private RaftStateName raftStateName = RaftStateName.FOLLOWER;

  public RaftStateName stateName() {
    return raftStateName;
  }

  public RaftState() {
  }

  public RaftState(RaftState state) {
    this.currentTerm = state.currentTerm;
    this.votedFor = state.votedFor;
    this.raftStateName = state.raftStateName;
  }

  public RaftState(long currentTerm, InfectedHost votedFor) {
    this.currentTerm = currentTerm;
    this.votedFor = votedFor;
  }

  public RaftState setRaftStateName(RaftStateName raftStateName) {
    this.raftStateName = raftStateName;
    return this;
  }

  public long getCurrentTerm() {
    return currentTerm;
  }

  public RaftState setCurrentTerm(long currentTerm) {
    this.currentTerm = currentTerm;
    return this;
  }

  public InfectedHost getVotedFor() {
    return votedFor;
  }

  public RaftState setVotedFor(InfectedHost votedFor) {
    this.votedFor = votedFor;
    return this;
  }

  @Override
  public String toString() {
    return "state=" + stateName() +
      ", currentTerm=" + currentTerm + ", " +
      "votedFor=" + votedFor.toString();

  }
}
