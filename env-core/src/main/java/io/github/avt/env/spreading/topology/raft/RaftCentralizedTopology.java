package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.meta.Network;
import io.github.avt.env.spreading.topology.p2p.PeerToPeerNetworkTopology;
import io.github.avt.env.util.Utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RaftCentralizedTopology implements Topology<CentralNode> {

  private final PeerToPeerNetworkTopology p2p;
  private final CentralNode centralNode;
  private final int port;

  private final AtomicLong term = new AtomicLong(0);
  private final AtomicReference<RaftState> currentState = new AtomicReference<>(new Follower());

  public RaftCentralizedTopology(int port, Network network, int p2pGossipDelay) {
    this.port = port;
    p2p = new PeerToPeerNetworkTopology(port, network, p2pGossipDelay);
    setupRoute();
    centralNode = new CentralNode();
  }

  public RaftCentralizedTopology(Network network) {
    this(Utils.pickRandomFreePort(), network, PeerToPeerNetworkTopology.DEFAULT_GOSSIP_PERIOD_MS);
  }

  private void setupRoute() {
    p2p.router().route("vote").handler(ctx -> {

    });
  }

  @Override
  public CentralNode topologyInformation() {
    return centralNode;
  }

  @Override
  public void runTopologyService(int envPort) {
    p2p.runTopologyService(envPort);
  }

  @Override
  public int topologyServicePort() {
    return port;
  }
}
