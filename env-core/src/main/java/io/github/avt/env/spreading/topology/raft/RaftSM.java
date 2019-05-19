package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.spreading.topology.p2p.ListOfPeers;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class RaftSM {

  private static final Logger log = LoggerFactory.getLogger(RaftSM.class);

  // immutable
  private final Vertx vertx;
  private final WebClient webClient;
  private final long heartBeatTimeout;
  private final ElectionTimoutModel electionTimoutModel;
  private final Object syncLock = new Object();

  // fields with a state
  private RaftState currentState = new Follower();
  private long electionTimerId = 0;
  private long term = 0;
  private final ListOfPeers peers;

  public RaftSM(Vertx vertx, ListOfPeers peers, ElectionTimoutModel electionTimoutModel, long heartBeatTimeout) {
    this.vertx = vertx;
    this.peers = peers;
    this.electionTimoutModel = electionTimoutModel;
    this.heartBeatTimeout = heartBeatTimeout;
    this.webClient = WebClient.create(vertx);
  }

  public void startRaftNode() {
    synchronized (syncLock) {
      startElectionTimer();
    }
  }

  private void startElectionTimer() {
    synchronized (syncLock) {
      if (currentState instanceof Follower || currentState instanceof Candidate) {
        long electionTimeout = electionTimoutModel.generateDelay();
        log.info("Staring election timeout on {}ms", electionTimeout);
        electionTimerId = vertx.setTimer(electionTimeout, event -> {
          log.info("Triggering becoming a candidate procedure");
          becomeCandidate();
        });
      } else {
        log.info("Can't start election timer since '{}'!='Follower'", currentState);
      }
    }
  }

  private void becomeCandidate() {
    synchronized (syncLock) {
      if (currentState instanceof Follower) {
        log.info("Becoming a candidate");
        currentState = new Candidate();
        requestVotes().setHandler(event -> {
          if (event.succeeded()) {
            ElectionResult result = event.result();
            log.info("Election result: {}", result);
          } else {
            log.error("An error during voting process occurred", event.cause());
          }
        });
      } else if (currentState instanceof Candidate) {
        log.info("Already a candidate");
      } else if (currentState instanceof Leader) {

      } else {
        log.error("Unknown state: {}", currentState);
      }
    }
  }

  public boolean voteRequested(VoteRequest voteRequest) {
    synchronized (syncLock) {
      return false;
    }
  }

  public Future<ElectionResult> requestVotes() {
    synchronized (syncLock) {
      Set<InfectedHost> hostToRequest = peers.currentPeers();
      int total = hostToRequest.size();
      hostToRequest.stream().map(infectedHost -> {
       return webClient.postAbs(String.format("http://%s:%d%s",
          infectedHost.getHostWithEnv().getHost(),
          infectedHost.topologyServicePort(),
          RaftCentralizedTopology.REQUEST_VOTE));
      });
      return Future.failedFuture("not implemented");
    }
  }

  public void appendEntriesReceived(AppendEntries entries) {
    synchronized (syncLock) {
      if (currentState instanceof Follower) {
        resetElectionTimeout();
      }
    }
  }

  private void resetElectionTimeout() {
    synchronized (syncLock) {
      vertx.cancelTimer(electionTimerId);
      startElectionTimer();
    }
  }
}
