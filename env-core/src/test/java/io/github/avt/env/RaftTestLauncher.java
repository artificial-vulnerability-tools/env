package io.github.avt.env;

import io.github.avt.env.extend.Launcher;
import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.TopologyInformation;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.spreading.meta.Network;
import io.github.avt.env.spreading.topology.raft.CentralNode;
import io.github.avt.env.spreading.topology.raft.RaftCentralizedTopology;
import io.github.avt.env.spreading.topology.raft.RandomizedRangeElectionTimoutModel;
import io.github.avt.env.util.Utils;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftTestLauncher extends Launcher {

  private static final Logger log = LoggerFactory.getLogger(RaftTestLauncher.class);

  @Override
  public Topology topology() {
    return new RaftCentralizedTopology(
      Vertx.vertx(new VertxOptions().setEventLoopPoolSize(8)),
      new RandomizedRangeElectionTimoutModel(500, 1000),
      50,
      Utils.pickRandomFreePort(),
      new Network(Network.NetworkType.LOCAL),
      2000
    );
  }

  @Override
  public synchronized void launch(int envPort) {
    TopologyInformation topologyInformation = topology.topologyInformation();
    var centralNode = (CentralNode) topologyInformation;
    centralNode.leaderElected((InfectedHost newLeader) -> {
      log.info("New leader elected at: {}", newLeader);
    });
  }
}
