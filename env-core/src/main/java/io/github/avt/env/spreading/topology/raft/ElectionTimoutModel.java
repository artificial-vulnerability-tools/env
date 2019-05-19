package io.github.avt.env.spreading.topology.raft;

public interface ElectionTimoutModel {

  long generateDelay();

}
