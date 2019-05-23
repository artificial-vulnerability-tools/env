package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.meta.Network;
import io.github.avt.env.spreading.topology.p2p.PeerToPeerNetworkTopology;
import io.github.avt.env.util.Utils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class RaftCentralizedTopology implements Topology<CentralNode> {

  private static final Logger log = LoggerFactory.getLogger(RaftCentralizedTopology.class);
  public static final String HEART_BEAT = "/heartBeat";
  public static final String REQUEST_VOTE = "/requestVote";

  private final PeerToPeerNetworkTopology p2p;
  private final CentralNode centralNode;
  private final Vertx vertx;
  private final ElectionTimoutModel electionTimoutModel;
  private final long heartbeatTimeout;
  private final int port;
  private RaftSM raftSM;

  public RaftCentralizedTopology(Vertx vertx, ElectionTimoutModel electionTimoutModel, long heartbeatTimeout, int port, Network network, int p2pGossipDelay) {
    this.vertx = vertx;
    this.electionTimoutModel = electionTimoutModel;
    this.heartbeatTimeout = heartbeatTimeout;
    this.port = port;
    p2p = new PeerToPeerNetworkTopology(port, network, p2pGossipDelay);
    setupRoute();
    centralNode = new CentralNode();
  }

  public RaftCentralizedTopology(Network network) {
    this(Vertx.vertx(), new RandomizedRangeElectionTimoutModel(150, 300), 20, Utils.pickRandomFreePort(), network, PeerToPeerNetworkTopology.DEFAULT_GOSSIP_PERIOD_MS);
  }

  private void setupRoute() {
    p2p.router().post(REQUEST_VOTE).handler(ctx -> {
      ctx.request().bodyHandler(body -> {
        final VoteRequest voteRequest = body.toJsonObject().mapTo(VoteRequest.class);
        final VoteResponse voteResponse = raftSM.voteRequested(voteRequest);
        ctx.response().end(Buffer.buffer(JsonObject.mapFrom(voteResponse).encodePrettily().getBytes(StandardCharsets.UTF_8)));
        log.info("Request vote request:\n{}\nresponse:\n{}", voteRequest, voteResponse);
      });
    });

    p2p.router().post(HEART_BEAT).handler(ctx -> {
      ctx.request().bodyHandler(body -> {
        final JsonObject entries = body.toJsonObject();
        final AppendEntries request = entries.mapTo(AppendEntries.class);
        final AppendEntriesResponse appendEntriesResponse = raftSM.appendEntriesReceived(request);
        ctx.response().end(Buffer.buffer(JsonObject.mapFrom(appendEntriesResponse).encodePrettily().getBytes(StandardCharsets.UTF_8)));
        log.info("Heartbeat request:\n{}\nresponse:\n{}", request, appendEntriesResponse);
      });
    });
  }

  @Override
  public CentralNode topologyInformation() {
    return centralNode;
  }

  @Override
  public void runTopologyService(int envPort) {
    p2p.runTopologyService(envPort);
    raftSM = new RaftSM(vertx, p2p.topologyInformation(), electionTimoutModel, heartbeatTimeout);
    raftSM.startRaftNode();
  }

  @Override
  public int topologyServicePort() {
    return port;
  }
}
