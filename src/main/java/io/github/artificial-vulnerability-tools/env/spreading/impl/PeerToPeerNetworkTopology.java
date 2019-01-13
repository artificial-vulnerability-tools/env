/*
 *
 * Copyright 2018 Pavel Drankou.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.avt.env.spreading.impl;

import com.github.avt.env.daemon.AVTService;
import com.github.avt.env.spreading.*;
import com.github.avt.env.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * This is a very simple Peer to Peer topology.
 * <p>
 * The topology require a HTTP service with 2 active threads.
 * <p>
 * The passive thread expose an POST /gossip endpoint,
 */
public class PeerToPeerNetworkTopology implements Topology {

  public final Logger log;
  public static final Integer DELAY = 1000;
  public static final Integer PEER_TO_PEER_TOPOLOGY_DEFAULT_PORT = 2222;
  private final WebClient webClient;
  private final Set<InfectedHost> peers = new ConcurrentHashSet<>();
  public final Integer topologyServicePort;
  private final Vertx vertx;
  private final InfectionClient infectionClient;
  private final GossipClient gossipClient;
  private final NetClient netClient;
  private final String networkHost;
  private volatile int envPort;
  private volatile InfectedHost thisPeer;

  private Random rnd = new Random();

  public PeerToPeerNetworkTopology(int topologyServicePort, Network network) {
    this.vertx = Vertx.vertx();
    this.netClient = vertx.createNetClient();
    this.webClient = WebClient.create(vertx);
    this.infectionClient = new InfectionClientImpl(vertx);
    this.topologyServicePort = topologyServicePort;
    this.networkHost = network.getHostAddressInTheNetworkBlocking();
    this.log = LoggerFactory.getLogger(PeerToPeerNetworkTopology.class.getName() + ":" + topologyServicePort);
    this.gossipClient = new GossipClientImpl(vertx);
  }

  public PeerToPeerNetworkTopology(Network network) {
    this(Utils.pickRandomFreePort(), network);
  }

  @Override
  public void runTopologyService(int envPort) {
    this.envPort = envPort;
    this.thisPeer = new InfectedHost(new HostWithEnvironment(networkHost, envPort), topologyServicePort);
    peers.add(thisPeer);
    notifyEnvironmentAboutTopologyService();
    startGossipPassiveService();
    startGossipActiveService();
  }

  private void notifyEnvironmentAboutTopologyService() {
    webClient.postAbs(String.format("http://localhost:%s%s", envPort, AVTService.VIRUS_TOPOLOGY_ON_PORT))
      .sendJsonObject(Utils.virusPortJson(topologyServicePort), response -> {
        if (response.succeeded()) {
          log.info("Successfully responded back with topology service port");
        } else {
          log.error("Unable to respond back with topology service port", response.cause());
        }
      });
  }

  @Override
  public int topologyServicePort() {
    return topologyServicePort;
  }

  private void startGossipActiveService() {
    runActiveServiceTimer();
  }

  private void runActiveServiceTimer() {
    vertx.setTimer(DELAY, id -> {
      log.info("Time to gossip with someone");
      gossipWithSomeOne().setHandler(result -> {
        log.info("One session of gossip has been ended");
        runActiveServiceTimer();
      });
    });
  }

  private Future<Void> gossipWithSomeOne() {
    Future<Void> gossipDone = Future.future();
    Optional<InfectedHost> optionSocketAddress = pickRandomPeer();
    if (optionSocketAddress.isEmpty()) {
      gossipDone.complete();
      return gossipDone;
    }
    InfectedHost gossipTarget = optionSocketAddress.get();
    infectionClient.topologyServicePort(gossipTarget.getHostWithEnv()).setHandler(res -> {
      if (res.succeeded()) {
        Optional<Integer> result = res.result();
        if (result.isPresent()) {
          Integer discoveredSerivcePort = result.get();
          var actualGossipTarget = new InfectedHost(gossipTarget.getHostWithEnv(), discoveredSerivcePort);
          log.info(String.format("Topology service detected: %s. Start gossiping....", actualGossipTarget));
          if (!gossipTarget.equals(actualGossipTarget)) {
            peers.remove(gossipTarget);
            peers.add(actualGossipTarget);
          }
          gossipClient.gossipWith(actualGossipTarget, peers).setHandler(gossipResponse -> {
            if (gossipResponse.succeeded()) {
              Set<InfectedHost> updates = gossipResponse.result();
              updatePeers(updates);
              gossipDone.complete();
            } else {
              log.error("Failed in attempt to gossip with " + actualGossipTarget, gossipResponse.cause());
              gossipDone.fail(gossipResponse.cause());
            }
          });
        } else {
          log.info("No topology service detected. Spread infection to " + gossipTarget.getHostWithEnv());
          infectionClient.infect(gossipTarget.getHostWithEnv()).setHandler(infectionResult -> {
            if (infectionResult.succeeded()) {
              log.info("Successfully infected: " + infectionResult.result());
              peers.remove(gossipTarget);
              peers.add(infectionResult.result());
              // TODO consider if we need to gossip at this point
              gossipDone.complete();
            } else {
              log.error("Unable to infect " + gossipTarget.getHostWithEnv());
              gossipDone.fail(infectionResult.cause());
            }
          });
        }
      } else {
        log.error("Unable to obtain topology service port from env: " + gossipTarget.getHostWithEnv(), res.cause());
        gossipDone.fail(res.cause());
      }
    });
    return gossipDone;
  }

  private void updatePeers(Set<InfectedHost> updates) {
    updates.forEach(update -> {
      boolean isNew = peers.add(update);
      if (isNew) {
        log.info("New peer discovered: " + update);
      }
    });
  }

  private Optional<InfectedHost> pickRandomPeer() {
    var setOfPeerToChoseFrom = new HashSet<>(peers);
    setOfPeerToChoseFrom.remove(thisPeer);
    if (setOfPeerToChoseFrom.size() > 0) {
      int size = peers.size();
      int item = rnd.nextInt(size);
      int i = 0;
      for (var obj : setOfPeerToChoseFrom) {
        if (i == item)
          return Optional.ofNullable(obj);
        i++;
      }
    }
    return Optional.empty();
  }

  private void startGossipPassiveService() {
    var httpServer = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.post("/gossip").handler(ctx -> {
      ctx.request().bodyHandler(body -> {
        var responsePeers = new HashSet<>(peers);
        JsonArray objects = body.toJsonArray();
        Set<InfectedHost> updates = objects.stream().map(o -> (String) o).map(InfectedHost::new).collect(Collectors.toSet());
        updatePeers(updates);
        var responseJson = new JsonArray();
        responsePeers.stream().map(InfectedHost::toString).forEach(responseJson::add);
        ctx.response().end(responseJson.toBuffer());
      });
    });
    httpServer.requestHandler(router::accept).listen(topologyServicePort, done -> {
      if (done.succeeded()) {
        log.info("topology service started on port " + topologyServicePort);
      } else {
        log.error("Unable to start topology service on port " + topologyServicePort);
      }
    });
  }
}
