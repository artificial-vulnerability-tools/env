package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;

public class Follower extends RaftState {

  public Follower(RaftState state) {
    super(state);
  }

  public Follower(long currentTerm, InfectedHost votedFor) {
    super(currentTerm, votedFor);
  }

  @Override
  public String stateName() {
    return "Follower";
  }

}
