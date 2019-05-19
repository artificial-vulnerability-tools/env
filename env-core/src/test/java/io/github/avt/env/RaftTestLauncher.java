package io.github.avt.env;

import io.github.avt.env.extend.Launcher;
import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.TopologyInformation;
import io.github.avt.env.spreading.meta.Network;
import io.github.avt.env.spreading.topology.raft.CentralNode;
import io.github.avt.env.spreading.topology.raft.RaftCentralizedTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RaftTestLauncher extends Launcher {

  private static final Logger log = LoggerFactory.getLogger(RaftTestLauncher.class);

  @Override
  public Topology topology() {
    return new RaftCentralizedTopology(new Network(Network.NetworkType.LOCAL));
  }

  @Override
  public synchronized void launch(int envPort) {
    TopologyInformation topologyInformation = topology.topologyInformation();
    var centralNode = (CentralNode) topologyInformation;
    Objects.requireNonNull(centralNode);
    centralNode.leaderElected(newLeader -> log.info("New leader elected at: {}", newLeader));
  }
}
