package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;

import java.util.Optional;

public class RaftState {

  private long currentTerm = 0;
  private Optional<InfectedHost> votedFor = Optional.empty();
  private RaftStateName raftStateName = RaftStateName.FOLLOWER;

  public String stateName() {
    return raftStateName.name();
  }

  public RaftState() {
  }

  public RaftState(RaftState state) {
    this.currentTerm = state.currentTerm;
    this.votedFor = state.votedFor;
    this.raftStateName = state.raftStateName;
  }

  public RaftState(long currentTerm, Optional<InfectedHost> votedFor) {
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

  public Optional<InfectedHost> getVotedFor() {
    return votedFor;
  }

  public void setVotedFor(Optional<InfectedHost> votedFor) {
    this.votedFor = votedFor;
  }

  @Override
  public String toString() {
    return "state=" + stateName() +
      ", currentTerm=" + currentTerm + ", " +
      "votedFor=" + (votedFor.isEmpty() ? "none" : votedFor.get().toString());

  }
}
