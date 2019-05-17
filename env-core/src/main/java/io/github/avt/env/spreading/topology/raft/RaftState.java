package io.github.avt.env.spreading.topology.raft;

public class RaftState {

  public static Follower FOLLOWER = new Follower();
  public static Leader LEADER = new Leader();
  public static Candidate CANDIDATE = new Candidate();

}
