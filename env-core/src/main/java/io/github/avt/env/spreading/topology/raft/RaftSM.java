package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.topology.p2p.ListOfPeers;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftSM {

  private static final Logger log = LoggerFactory.getLogger(RaftSM.class);

  private RaftState currentState = new Follower();
  private long electionTimerId = 0;
  private final Vertx vertx;
  private final ListOfPeers peers;
  private final ElectionTimoutModel electionTimoutModel;
  private final long heartBeatTimeout;

  private final Object syncLock = new Object();

  public RaftSM(Vertx vertx, ListOfPeers peers, ElectionTimoutModel electionTimoutModel, long heartBeatTimeout) {
    this.vertx = vertx;
    this.peers = peers;
    this.electionTimoutModel = electionTimoutModel;
    this.heartBeatTimeout = heartBeatTimeout;
  }

  public void startRaftNode() {
    synchronized (syncLock) {
      startElectionTimer();
    }
  }

  private void startElectionTimer() {
    synchronized (syncLock) {
      if (currentState instanceof Follower) {
        long electionTimeout = electionTimoutModel.generateDelay();
        log.info("Staring election timeout on '{}' seconds", electionTimeout);
        electionTimerId = vertx.setTimer(electionTimeout, event -> {
          log.info("Triggering becoming a candidate procedure");
          becomeCandidate();
        });
      }
    }
  }

  private void becomeCandidate() {
    synchronized (syncLock) {
      if (currentState instanceof Follower) {
        log.info("Becoming a candidate");
        currentState = new Candidate();
        requestVotes().setHandler(event -> {

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
