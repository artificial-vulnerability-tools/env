package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;

public class Leader extends RaftState {

  public Leader(RaftState state) {
    super(state);
  }

  public Leader(long currentTerm, InfectedHost votedFor) {
    super(currentTerm, votedFor);
  }

  @Override
  public String stateName() {
    return "Leader";
  }

}
