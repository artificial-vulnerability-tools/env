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
import com.github.avt.env.spreading.InfectedHost;
import com.github.avt.env.spreading.InfectionClient;
import com.github.avt.env.spreading.Network;
import com.github.avt.env.spreading.Topology;
import com.github.avt.env.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.github.avt.env.daemon.AVTService.PORT_FIELD;


/**
 * This is a very simple Peer to Peer topology.
 * <p>
 * The topology require a HTTP service with 2 active threads.
 * <p>
 * The passive thread expose an POST /gossip endpoint,
 */
public class PeerToPeerNetworkTopology implements Topology {

  public static final Logger log = LoggerFactory.getLogger(PeerToPeerNetworkTopology.class);
  public static final Integer DELAY = 1000;
  public static final Integer PEER_TO_PEER_TOPOLOGY_DEFAULT_PORT = 2222;
  private final WebClient webClient;
  private Set<InfectedHost> peers = new ConcurrentHashSet<>();
  public final Integer topologyServicePort;
  private final Vertx vertx;
  private final InfectionClient infectionClient;
  private final NetClient netClient;
  private final String networkHost;
  private int envPort;

  private Random rnd = new Random();

  public PeerToPeerNetworkTopology(int topologyServicePort, Network network) {
    this.vertx = Vertx.vertx();
    this.netClient = vertx.createNetClient();
    this.webClient = WebClient.create(vertx);
    this.infectionClient = new InfectionClientImpl(vertx);
    this.topologyServicePort = topologyServicePort;
    this.networkHost = network.getHostAddressInTheNetworkBlocking();
  }

  public PeerToPeerNetworkTopology(Network network) {
    this(Utils.pickRandomFreePort(), network);
  }

  @Override
  public void runTopologyService(int envPort) {
    this.envPort = envPort;
    notifyEnvironmentAboutTopologyService();
    startGossipPassiveService();
    startGossipActiveService();
  }

  private void notifyEnvironmentAboutTopologyService() {
    webClient.postAbs(String.format("http://localhost:%s%s", envPort, AVTService.VIRUS_TOPOLOGY_ON_PORT))
      .sendJsonObject(new JsonObject().put(PORT_FIELD, topologyServicePort), response -> {
        if (response.succeeded()) {
          log.error("Successfully responded back with topology service port");
        } else {
          log.error("Unable to respond back with topology service port");
        }
      });
  }

  @Override
  public int topologyServicePort() {
    return topologyServicePort;
  }

  private void startGossipActiveService() {
    vertx.setPeriodic(DELAY, event -> {
      Optional<InfectedHost> optionSocketAddress = pickRandomPeer();
      if (optionSocketAddress.isEmpty()) {
        return;
      }
      InfectedHost gossipTarget = optionSocketAddress.get();
      checkIfNeedToInfect(gossipTarget)
        .compose(needToInfect -> {
          if (needToInfect) {
            return infectionClient.infect(gossipTarget.getHostWithEnv());
          } else {
            return gossip(gossipTarget);
          }
        });
    });
  }

  private Future<Void> gossip(InfectedHost gossipTarget) {
    return null;
  }

  private Future<Boolean> checkIfNeedToInfect(InfectedHost gossipTarget) {
    Future<Boolean> result = Future.future();
    netClient.connect(gossipTarget.toVertxSocketAddress(), event -> result.complete(event.failed()));
    return result;
  }

  private Optional<InfectedHost> pickRandomPeer() {
    int size = peers.size();
    int item = rnd.nextInt(size); // In real life, the Random object should be rather more shared than this
    int i = 0;
    for (var obj : peers) {
      if (i == item)
        return Optional.ofNullable(obj);
      i++;
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
        objects.stream().map(o -> (String) o).forEach(uri -> {
          var infectedHost = new InfectedHost(uri);
          peers.add(infectedHost);
        });
        var responseJson = new JsonArray();
        responsePeers.forEach(responseJson::add);
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
