package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.TopologyInformation;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.vertx.core.Handler;

import java.util.Objects;


public class CentralNode implements TopologyInformation {

  private Handler<InfectedHost> leader;
  private InfectedHost currentLeader;

  public synchronized CentralNode leaderElected(Handler<InfectedHost> leader) {
    this.leader = leader;
    return this;
  }

  public synchronized InfectedHost currentLeader() {
    return currentLeader;
  }

  public synchronized void successHeartBeatFrom(InfectedHost host) {
    Objects.requireNonNull(host);
    if (!host.equals(currentLeader) && leader != null) {
      currentLeader = host;
      leader.handle(currentLeader);
    }
  }
}
