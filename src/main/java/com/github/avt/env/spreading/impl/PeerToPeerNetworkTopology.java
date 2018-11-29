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
import com.github.avt.env.spreading.Topology;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.github.avt.env.daemon.AVTService.INFECT_PATH;
import static com.github.avt.env.daemon.AVTService.NAME_OF_JAR_WITH_VIRUS;


/**
 * This is a very simple Peer to Peer topology.
 * <p>
 * The topology require a HTTP service with 2 active threads.
 * <p>
 * The passive thread expose an POST /gossip endpoint,
 */
public class PeerToPeerNetworkTopology implements Topology {

  public static final Logger log = LoggerFactory.getLogger(PeerToPeerNetworkTopology.class);

  private Set<SocketAddress> peers = new ConcurrentHashSet<>();
  public static final Integer VIRUS_PORT = 2223;
  public static final Integer DELAY = 1000;
  public static final String NOT_A_PEER_PARAM = "not-a-peer";
  private final Vertx vertx;
  private final WebClient httpClient;
  private final NetClient netClient;

  private Random rnd = new Random();

  public PeerToPeerNetworkTopology() {
    this.vertx = Vertx.vertx();
    this.httpClient = WebClient.create(vertx);
    this.netClient = vertx.createNetClient();
  }

  @Override
  public void runTopologyService() {
    startGossipPassiveService();
    startGossipActiveService();
  }

  private void startGossipActiveService() {
    vertx.setPeriodic(DELAY, event -> {
      Optional<SocketAddress> optionSocketAddress = pickRandomPeer();
      if (optionSocketAddress.isEmpty()) {
        return;
      }

      SocketAddress gossipTarget = optionSocketAddress.get();
      checkIfNeedToInfect(gossipTarget)
        .compose(needToInfect -> {
          if (needToInfect) {
            return infect(gossipTarget);
          } else {
            return gossip(gossipTarget);
          }
        });
    });
  }

  private Future<Void> gossip(SocketAddress gossipTarget) {
    return null;
  }

  private Future<Void> infect(SocketAddress gossipTarget) {
    Future<Void> result = Future.future();
    vertx.fileSystem().open(NAME_OF_JAR_WITH_VIRUS, new OpenOptions(), fileRes -> {
      if (fileRes.succeeded()) {
        ReadStream<Buffer> fileStream = fileRes.result();
        httpClient
          .post(AVTService.DEFAULT_PORT, gossipTarget.host(), INFECT_PATH)
          .sendStream(fileStream, ar -> {
            if (ar.succeeded()) {
              log.info(gossipTarget.host() + AVTService.DEFAULT_PORT);
              result.complete();
            } else {
              result.fail(ar.cause());
            }
          });
      } else {
        result.fail(fileRes.cause());
      }
    });
    return result;
  }

  private Future<Boolean> checkIfNeedToInfect(SocketAddress gossipTarget) {
    Future<Boolean> result = Future.future();
    netClient.connect(gossipTarget, event -> result.complete(event.succeeded()));
    return result;
  }

  private Optional<SocketAddress> pickRandomPeer() {
    int size = peers.size();
    int item = rnd.nextInt(size); // In real life, the Random object should be rather more shared than this
    int i = 0;
    for (SocketAddress obj : peers) {
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
      MultiMap params = ctx.request().params();
      if (!params.contains(NOT_A_PEER_PARAM)) {
        peers.add(ctx.request().connection().remoteAddress());
      }

      ctx.request().bodyHandler(body -> {
        JsonArray objects = body.toJsonArray();
        objects.stream().map(o -> (String) o).forEach(uri -> {
          String[] split = uri.split(":");
          peers.add(new SocketAddressImpl(Integer.parseInt(split[1]), split[0]));
        });
      });
    });
    httpServer.requestHandler(router::accept).listen(VIRUS_PORT);
  }
}
