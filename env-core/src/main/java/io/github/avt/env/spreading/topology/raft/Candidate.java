package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;

public class Candidate extends RaftState {

  public Candidate(RaftState state) {
    super(state);
  }

  public Candidate(long currentTerm, InfectedHost votedFor) {
    super(currentTerm, votedFor);
  }

  @Override
  public String stateName() {
    return "Candidate";
  }
}
