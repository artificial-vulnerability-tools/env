package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.spreading.topology.p2p.ListOfPeers;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RaftSM {

  private static final Logger log = LoggerFactory.getLogger(RaftSM.class);

  // immutable
  private final Vertx vertx;
  private final WebClient webClient;
  private final long heartBeatTimeout;
  private final ElectionTimoutModel electionTimoutModel;
  private final Object syncLock = new Object();

  // fields with a state
  private RaftState currentState = new RaftState();
  private long electionTimerId = 0;

  private final ListOfPeers peers;

  private ElectionResult lastElections = null;

  public RaftSM(Vertx vertx, ListOfPeers peers, ElectionTimoutModel electionTimoutModel, long heartBeatTimeout) {
    this.vertx = vertx;
    this.peers = peers;
    this.electionTimoutModel = electionTimoutModel;
    this.heartBeatTimeout = heartBeatTimeout;
    this.webClient = WebClient.create(vertx);
  }

  public RaftState getCurrentState() {
    return currentState;
  }

  public void startRaftNode() {
    synchronized (syncLock) {
      resetElectionTimeout();
    }
  }

  private void resetElectionTimeout() {
    synchronized (syncLock) {
      if (isFollower() || isCandidate()) {
        vertx.cancelTimer(electionTimerId);
        long electionTimeout = electionTimoutModel.generateDelay();
        log.info("Staring election timeout on {}ms", electionTimeout);
        electionTimerId = vertx.setTimer(electionTimeout, event -> {
          log.info("Triggering becoming a candidate procedure");
          becomeCandidate();
        });
      } else {
        log.error("Can't reset election timer since '{}'!='Follower' or 'Candidate'", currentState.stateName());
      }
    }
  }

  private void becomeCandidate() {
    synchronized (syncLock) {
      if (isFollower()) {
        changeStateToCandidate();
        requestVotes().setHandler(event -> {
          if (event.succeeded()) {
            ElectionResult result = event.result();
            lastElections = result;
            log.info("Election result: {}", result);
            if (result.isQuorum()) {
              becomeLeader();
            } else {
              log.info("Failed to became a leader. Reason: no quorum. Getting back to a follower state");
              backToFollowerState();
            }
          } else {
            log.error("An error during voting process occurred", event.cause());
          }
        });
      } else if (isCandidate()) {
        log.error("Already a candidate");
      } else if (isLeader()) {
        log.error("Leader can't become a candidate");
      } else {
        log.error("Unknown state: {}", currentState);
      }
    }
  }

  private void backToFollowerState() {
    log.info("Getting back to the follower state");
    changeStateTo(RaftStateName.FOLLOWER);
    resetElectionTimeout();
  }

  private void changeStateToCandidate() {
    changingState(cur -> {
      log.info("Becoming a candidate");
      cur.setCurrentTerm(currentState.getCurrentTerm() + 1);
      cur.setVotedFor(Optional.of(peers.thisPeer()));
      cur.setRaftStateName(RaftStateName.CANDIDATE);
    });
  }


  private void changeStateTo(RaftStateName name) {
    changingState(cur -> cur.setRaftStateName(name));
  }

  private void changingState(Handler<RaftState> changeFun) {
    synchronized (syncLock) {
      RaftState newOne = new RaftState(currentState);
      changeFun.handle(newOne);
      changeRaftState(newOne);
    }
  }

  private void becomeLeader() {
    synchronized (syncLock) {
      if (isCandidate()) {
        log.info("Becoming a leader");
        changeStateTo(RaftStateName.LEADER);
        startSendingHeartBeats();
      } else {
        log.error("'{}' can't become a leader. Only a candidate can", currentState.stateName().toString());
      }
    }
  }

  private void startSendingHeartBeats() {
    synchronized (syncLock) {
      vertx.setPeriodic(heartBeatTimeout, event -> {
        synchronized (syncLock) {
          final JsonObject body = JsonObject.mapFrom(heartBeatRequest());
          if (isLeader()) {
            final Set<InfectedHost> infectedHosts = peers.onlyPeersWithTopologyService();
            log.debug("Sending heartbeats to {} nodes.", infectedHosts.size());
            final List<Future> collect = infectedHosts.stream().map(infectedHost -> {
              Future<HttpResponse<Buffer>> res = Future.future();
              webClient.postAbs(
                String.format("http://%s:%d%s", infectedHost.getHostWithEnv().getHost(), infectedHost.topologyServicePort(), RaftCentralizedTopology.HEART_BEAT)
              ).sendJsonObject(body, res);
              return res;
            }).collect(Collectors.toList());
            CompositeFuture.all(collect).map(compositeFuture -> {
              var heartBeatOk = compositeFuture.list().stream()
                .map(o -> (HttpResponse<Buffer>) o)
                .map(response -> response.bodyAsJsonObject().mapTo(HeartBeatResponse.class))
                .filter(HeartBeatResponse::isSuccess)
                .count();
              return new HeartbeatResult(heartBeatOk, (long) collect.size());
            }).setHandler(result -> {
              synchronized (syncLock) {
                if (result.succeeded() && isLeader()) {
                  final HeartbeatResult heartbeat = result.result();
                  log.debug("Heartbeat result: " + heartbeat);
                  if (!heartbeat.isQuorum()) {
                    log.info("Not leading a majority of nodes.");
                    backToFollowerState();
                  } else {
                    log.debug("Holding a majority");
                  }
                } else {
                  log.debug("Heartbeat failed: ", result.cause());
                }
              }
            });
          }
        }
      });
    }
  }

  private HeartBeatRequest heartBeatRequest() {
    synchronized (syncLock) {
      return new HeartBeatRequest(currentState.getCurrentTerm(), peers.thisPeer().toString());
    }
  }

  public VoteResponse voteRequested(VoteRequest voteRequest) {
    synchronized (syncLock) {
      if (currentState.getCurrentTerm() < voteRequest.getTerm()) {
        log.info("Voting for {}", voteRequest.getCandidate());
        changingState(cur -> {
          cur.setRaftStateName(RaftStateName.FOLLOWER);
          cur.setCurrentTerm(voteRequest.getTerm());
          cur.setVotedFor(Optional.of(new InfectedHost(voteRequest.getCandidate())));
        });
        resetElectionTimeout();
        return new VoteResponse(currentState.getCurrentTerm(), true);
      } else {
        log.info("Not voting for {}", voteRequest.getCandidate());
        return new VoteResponse(currentState.getCurrentTerm(), false);
      }
    }
  }

  public Future<ElectionResult> requestVotes() {
    synchronized (syncLock) {
      Set<InfectedHost> hostToRequest = peers.onlyPeersWithTopologyService();
      List<Future> futures = hostToRequest.stream().map(infectedHost ->
        webClient.postAbs(String.format("http://%s:%d%s",
          infectedHost.getHostWithEnv().getHost(),
          infectedHost.topologyServicePort(),
          RaftCentralizedTopology.REQUEST_VOTE)))
        .map(request -> {
          Future<HttpResponse<Buffer>> res = Future.future();
          VoteRequest vote = new VoteRequest(currentState.getCurrentTerm(), peers.thisPeer().toString());
          request.sendJsonObject(JsonObject.mapFrom(vote), res);
          return res;
        }).collect(Collectors.toList());
      return CompositeFuture.all(futures).map(compositeFuture -> {
        long votedForThisNode = compositeFuture.list().stream()
          .map(o -> (HttpResponse<Buffer>) o)
          .map(response -> response.bodyAsJsonObject().mapTo(VoteResponse.class))
          .filter(VoteResponse::isVoteGranted)
          .count();
        return new ElectionResult(votedForThisNode + 1, (long) futures.size() + 1);
      });
    }
  }

  public HeartBeatResponse heartBeatReceived(HeartBeatRequest request) {
    synchronized (syncLock) {
      InfectedHost infectedHost = new InfectedHost(request.getLeaderId());
      long term = request.getTerm();
      // if voted this term candidate
      if (currentState.getVotedFor().equals(Optional.of(infectedHost)) && currentState.getCurrentTerm() == term) {
        resetElectionTimeout();
        return new HeartBeatResponse(currentState.getCurrentTerm(), true);
      }
      // if term simply higher
      else if (currentState.getCurrentTerm() < term) {
        log.info("Received heartbeat with higher term... following");
        changingState(cur -> {
          cur.setRaftStateName(RaftStateName.FOLLOWER);
          cur.setCurrentTerm(term);
          cur.setVotedFor(Optional.of(infectedHost));
        });
        resetElectionTimeout();
        return new HeartBeatResponse(currentState.getCurrentTerm(), true);
      } else {
        return new HeartBeatResponse(currentState.getCurrentTerm(), false);
      }
    }
  }

  private boolean isCandidate() {
    return currentState.stateName().equals(RaftStateName.CANDIDATE.name());
  }

  private boolean isLeader() {
    return currentState.stateName().equals(RaftStateName.LEADER.name());
  }

  private boolean isFollower() {
    return currentState.stateName().equals(RaftStateName.FOLLOWER.name());
  }

  private void changeRaftState(RaftState newState) {
    synchronized (syncLock) {
      RaftState oldState = currentState;
      currentState = newState;
      log.info("State changed [{}]->[{}]", oldState.toString(), newState.toString());
    }
  }
}
