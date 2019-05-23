package io.github.avt.env.spreading.topology.raft;

import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.spreading.topology.p2p.ListOfPeers;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
  private RaftState currentState = new Follower();
  private long electionTimerId = 0;
  private long currentTerm = 0;
  private final ListOfPeers peers;
  private InfectedHost votedFor = null;
  private ElectionResult lastElections = null;

  public RaftSM(Vertx vertx, ListOfPeers peers, ElectionTimoutModel electionTimoutModel, long heartBeatTimeout) {
    this.vertx = vertx;
    this.peers = peers;
    this.electionTimoutModel = electionTimoutModel;
    this.heartBeatTimeout = heartBeatTimeout;
    this.webClient = WebClient.create(vertx);
    peers.newPeerHandler(peer -> {
      if (lastElections != null) {
        reElect();
      } else {
        reElect();
      }
    });

    vertx.setPeriodic(1000, event -> {
      synchronized (syncLock) {
        log.info("Current raft state: term='{}', peers='{}', state='{}', votedFor='{}'",
          currentTerm,
          peers.onlyPeersWithTopologyService().stream().map(InfectedHost::toString).collect(Collectors.joining(", ")),
          currentState.toString(),
          votedFor
        );
      }
    });
  }

  public RaftState getCurrentState() {
    return currentState;
  }

  public void startRaftNode() {
    synchronized (syncLock) {
      startElectionTimer();
    }
  }

  private void startElectionTimer() {
    synchronized (syncLock) {
      if (isFollower() || isCandidate()) {
        long electionTimeout = electionTimoutModel.generateDelay();
        log.info("Staring election timeout on {}ms", electionTimeout);
        vertx.cancelTimer(electionTimerId);
        electionTimerId = vertx.setTimer(electionTimeout, event -> {
          log.info("Triggering becoming a candidate procedure");
          becomeCandidate();
        });
      } else {
        log.info("Can't start election timer since '{}'!='Follower' or 'Candidate'", currentState);
      }
    }
  }

  private void becomeCandidate() {
    synchronized (syncLock) {
      if (isFollower()) {
        currentTerm++;
        log.info("Becoming a candidate");
        currentState = new Candidate();
        requestVotes().setHandler(event -> {
          if (event.succeeded()) {
            ElectionResult result = event.result();
            log.info("Election result: {}", result);
            if (result.isQuorum()) {
              becomeLeader();
            } else {
              startElectionTimer();
            }
          } else {
            log.error("An error during voting process occurred", event.cause());
          }
        });
      } else if (isCandidate()) {
        log.info("Already a candidate");
      } else if (isLeader()) {
        log.error("Leader can't become a candidate");
      } else {
        log.error("Unknown state: {}", currentState);
      }
    }
  }

  private void becomeLeader() {
    synchronized (syncLock) {
      if (isCandidate()) {
        log.info("Becoming a leader");
        currentState = new Leader();
        votedFor = null;
        startSendingHeartBeats();
      } else {
        log.error("'{}' can't become a leader", currentState.toString());
      }
    }
  }

  private void startSendingHeartBeats() {
    synchronized (syncLock) {
      vertx.setPeriodic(heartBeatTimeout, event -> {
        synchronized (syncLock) {
          final JsonObject body = JsonObject.mapFrom(generateAppendEntries());
          if (isLeader()) {
            final Set<InfectedHost> infectedHosts = peers.onlyPeersWithTopologyService();
            log.info("Sending heartbeats to {} nodes.", infectedHosts.size());
            final List<Future> collect = infectedHosts.stream().map(infectedHost -> {
              Future<HttpResponse<Buffer>> res = Future.future();
              webClient.postAbs(
                String.format("http://%s:%d%s", infectedHost.getHostWithEnv().getHost(), infectedHost.topologyServicePort(), RaftCentralizedTopology.HEART_BEAT)
              ).sendJsonObject(body, res);
              return res;
            }).collect(Collectors.toList());
            CompositeFuture.all(collect).map(compositeFuture -> {

              final List<AppendEntriesResponse> responses = compositeFuture.list().stream()
                .map(o -> (HttpResponse<Buffer>) o)
                .map(response -> response.bodyAsJsonObject().mapTo(AppendEntriesResponse.class))
                .collect(Collectors.toList());
              for (AppendEntriesResponse response : responses) {
                if (!response.isSuccess() && response.getTerm() >= currentTerm) {
                  reElect();
                  break;
                }
              }

              long heartBeatOk = responses.stream().filter(AppendEntriesResponse::isSuccess)
                .count();
              return new HeartbeatResult(heartBeatOk, (long) collect.size());
            }).setHandler(result -> {
              if (result.succeeded()) {
                final HeartbeatResult res = result.result();
                log.info("Heartbeat result: " + res);
              } else {
                log.error("Heartbeat failed: ", result.cause());
              }
            });
          }
        }
      });
    }
  }

  private AppendEntries generateAppendEntries() {
    synchronized (syncLock) {
      return new AppendEntries(currentTerm, peers.thisPeer().toString());
    }
  }

  public VoteResponse voteRequested(VoteRequest voteRequest) {
    synchronized (syncLock) {
      if (currentTerm < voteRequest.getTerm()) {
        log.info("Voting for {}", voteRequest.getCandidate());
        currentTerm = voteRequest.getTerm();
        votedFor = new InfectedHost(voteRequest.getCandidate());
        startElectionTimer();
        return new VoteResponse(currentTerm, true);
      } else {
        log.info("Not voting for {}", voteRequest.getCandidate());
        return new VoteResponse(currentTerm, false);
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
          VoteRequest vote = new VoteRequest(currentTerm, peers.thisPeer().toString());
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

  public AppendEntriesResponse appendEntriesReceived(AppendEntries entries) {
    synchronized (syncLock) {
      if (entries.getTerm() < currentTerm) {
        log.info("Received term('{}') lower than current('{}')", entries.getTerm(), currentTerm);
        return new AppendEntriesResponse(currentTerm, false);
      } else if (entries.getTerm() > currentTerm) {
        log.info("Received term('{}') bigger than current('{}')", entries.getTerm(), currentTerm);
        return startFollow(entries.getTerm(), new InfectedHost(entries.getLeaderId()));
      } else if (new InfectedHost(entries.getLeaderId()).equals(votedFor)) {
        log.info("success heartbeat from leader");
        return new AppendEntriesResponse(currentTerm, true);
      } else {
        log.info("few leaders situation detected... becoming a follower");
        reElect();
        return new AppendEntriesResponse(currentTerm, false);
      }
    }
  }

  private void reElect() {
    startFollow(currentTerm, null);
  }

  private boolean isCandidate() {
    return currentState instanceof Candidate;
  }

  private boolean isLeader() {
    return currentState instanceof Leader;
  }

  private boolean isFollower() {
    return currentState instanceof Follower;
  }

  private AppendEntriesResponse startFollow(long term, InfectedHost host) {
    synchronized (syncLock) {
      log.info("Start following '{}'", host);
      votedFor = host;
      currentTerm = term;
      currentState = new Follower();
      startElectionTimer();
      return new AppendEntriesResponse(currentTerm, true);
    }
  }
}
