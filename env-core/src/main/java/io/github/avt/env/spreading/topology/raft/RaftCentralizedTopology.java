package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.meta.Network;
import io.github.avt.env.spreading.topology.p2p.PeerToPeerNetworkTopology;
import io.github.avt.env.util.Utils;
import io.vertx.core.Vertx;

import java.util.concurrent.atomic.AtomicReference;

public class RaftCentralizedTopology implements Topology<CentralNode> {

  private final PeerToPeerNetworkTopology p2p;
  private final CentralNode centralNode;
  private final int port;
  public static final String REQUEST_VOTE = "/requestVote";

  private final AtomicReference<RaftState> currentState = new AtomicReference<>(new Follower());
  private final RaftSM raftSM;

  public RaftCentralizedTopology(Vertx vertx, ElectionTimoutModel electionTimoutModel, long heartbeatTimeout, int port, Network network, int p2pGossipDelay) {
    this.port = port;
    p2p = new PeerToPeerNetworkTopology(port, network, p2pGossipDelay);
    setupRoute();
    centralNode = new CentralNode();
    raftSM = new RaftSM(vertx, p2p.topologyInformation(), electionTimoutModel, heartbeatTimeout);
  }

  public RaftCentralizedTopology(Network network) {
    this(Vertx.vertx(), new RandomizedRangeElectionTimoutModel(150, 300), 20, Utils.pickRandomFreePort(), network, PeerToPeerNetworkTopology.DEFAULT_GOSSIP_PERIOD_MS);
  }

  private void setupRoute() {
    p2p.router().route(REQUEST_VOTE).handler(ctx -> {
      ctx.response().end("OK");
    });
  }

  @Override
  public CentralNode topologyInformation() {
    return centralNode;
  }

  @Override
  public void runTopologyService(int envPort) {
    p2p.runTopologyService(envPort);
    raftSM.startRaftNode();
  }

  @Override
  public int topologyServicePort() {
    return port;
  }
}
