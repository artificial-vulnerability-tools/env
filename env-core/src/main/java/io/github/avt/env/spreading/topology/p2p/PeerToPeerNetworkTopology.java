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

package io.github.avt.env.spreading.topology.p2p;

import io.github.avt.env.daemon.AVTService;
import io.github.avt.env.spreading.GossipClient;
import io.github.avt.env.spreading.InfectionClient;
import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.impl.GossipClientImpl;
import io.github.avt.env.spreading.impl.InfectionClientImpl;
import io.github.avt.env.spreading.meta.HostWithEnvironment;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.spreading.meta.Network;
import io.github.avt.env.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class PeerToPeerNetworkTopology implements Topology<ListOfPeers> {

  public final Logger log;
  public static final Integer DEFAULT_GOSSIP_PERIOD_MS = 1000;
  private final WebClient webClient;
  public final Integer topologyServicePort;
  private final Vertx vertx = Vertx.vertx();
  private final InfectionClient infectionClient;
  private final GossipClient gossipClient;
  private final String networkHost;
  private volatile int envPort;
  private volatile ListOfPeers listOfPeers;
  private final Integer gossipPeriodMs;
  Router router = Router.router(vertx);

  private Random rnd = new Random();

  public PeerToPeerNetworkTopology(int topologyServicePort, Network network, Integer gossipPeriodMs) {
    this.gossipPeriodMs = gossipPeriodMs;
    this.webClient = WebClient.create(vertx);
    this.infectionClient = new InfectionClientImpl(vertx);
    this.topologyServicePort = topologyServicePort;
    this.networkHost = network.getHostAddressInTheNetworkBlocking();
    this.log = LoggerFactory.getLogger(PeerToPeerNetworkTopology.class.getName() + ":" + topologyServicePort);
    this.gossipClient = new GossipClientImpl(vertx);
  }

  public PeerToPeerNetworkTopology(Network network) {
    this(Utils.pickRandomFreePort(), network, DEFAULT_GOSSIP_PERIOD_MS);
  }

  @Override
  public synchronized ListOfPeers topologyInformation() {
    return listOfPeers;
  }

  @Override
  public synchronized void runTopologyService(int envPort) {
    this.envPort = envPort;
    InfectedHost thisPeer = new InfectedHost(new HostWithEnvironment(networkHost, envPort), topologyServicePort);
    this.listOfPeers = new ListOfPeers(vertx, thisPeer);
    log.info("Peer list has been initialized");
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
    vertx.setTimer(gossipPeriodMs, id -> {
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
            listOfPeers.removePeer(gossipTarget);
            listOfPeers.addPeer(actualGossipTarget);
          }
          gossipClient.gossipWith(actualGossipTarget, listOfPeers.fullPeerList()).setHandler(gossipResponse -> {
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
              listOfPeers.removePeer(gossipTarget);
              listOfPeers.addPeer(infectionResult.result());
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

  public Router router() {
    return router;
  }

  private void updatePeers(Set<InfectedHost> updates) {
    updates.forEach(update -> {
      boolean isNew = listOfPeers.addPeer(update);
      if (isNew && update.topologyServicePort() != InfectedHost.NOT_INFECTED) {
        log.info("New peer discovered: " + update);
      }
    });
  }

  private Optional<InfectedHost> pickRandomPeer() {
    var setOfPeerToChoseFrom = listOfPeers.currentPeers();
    if (setOfPeerToChoseFrom.size() > 0) {
      int size = setOfPeerToChoseFrom.size();
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
    router.post("/gossip").handler(ctx -> {
      ctx.request().bodyHandler(body -> {
        var responsePeers = listOfPeers.fullPeerList();
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
