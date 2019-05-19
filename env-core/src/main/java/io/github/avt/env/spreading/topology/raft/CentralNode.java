package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.TopologyInformation;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.vertx.core.Handler;


public class CentralNode implements TopologyInformation {

  private Handler<InfectedHost> leader;

  public CentralNode leaderElected(Handler<InfectedHost> leader) {
    this.leader = leader;
    return this;
  }

  public InfectedHost currentLeader() {
    throw new IllegalStateException("Not implemented");
  }
}
