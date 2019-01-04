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

import com.github.avt.env.spreading.InfectedHost;
import com.github.avt.env.spreading.InfectionClient;
import com.github.avt.env.spreading.Topology;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.NetClient;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.Set;


/**
 * This is a very simple Peer to Peer topology.
 * <p>
 * The topology require a HTTP service with 2 active threads.
 * <p>
 * The passive thread expose an POST /gossip endpoint,
 */
public class PeerToPeerNetworkTopology implements Topology {

  public static final Logger log = LoggerFactory.getLogger(PeerToPeerNetworkTopology.class);

  private Set<InfectedHost> peers = new ConcurrentHashSet<>();
  public static final Integer VIRUS_PORT = 2223;
  public static final Integer DELAY = 1000;
  private final Vertx vertx;
  private final InfectionClient infectionClient;
  private final NetClient netClient;

  private Random rnd = new Random();

  public PeerToPeerNetworkTopology() {
    this.vertx = Vertx.vertx();
    this.netClient = vertx.createNetClient();
    this.infectionClient = new InfectionClientImpl(vertx);
  }

  @Override
  public void runTopologyService() {
    startGossipPassiveService();
    startGossipActiveService();
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
        JsonArray objects = body.toJsonArray();
        objects.stream().map(o -> (String) o).forEach(uri -> {
          InfectedHost infectedHost = new InfectedHost(uri);


        });
      });
    });
    httpServer.requestHandler(router::accept).listen(VIRUS_PORT);
  }
}
