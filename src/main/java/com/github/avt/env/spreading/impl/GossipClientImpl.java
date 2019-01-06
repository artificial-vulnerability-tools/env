package com.github.avt.env.spreading.impl;

import com.github.avt.env.spreading.GossipClient;
import com.github.avt.env.spreading.InfectedHost;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class GossipClientImpl implements GossipClient {

  private final static Logger log = LoggerFactory.getLogger(GossipClientImpl.class);

  private final Vertx vertx;
  private final WebClient webClient;

  public GossipClientImpl(Vertx vertx) {
    this.vertx = vertx;
    webClient = WebClient.create(vertx);
  }

  public GossipClientImpl() {
    this(Vertx.vertx());
  }

  @Override
  public Future<Set<InfectedHost>> gossipWith(InfectedHost hostToGossipWith, Set<InfectedHost> info) {
    var jsonToSend = new JsonArray();
    info.stream().map(InfectedHost::toString).forEach(jsonToSend::add);
    var result = Future.<Set<InfectedHost>>future();
    webClient.postAbs(String.format("http://%s:%d/gossip", hostToGossipWith.getHostWithEnv().getHost(), hostToGossipWith.getVirusPort()))
      .sendJson(jsonToSend, event -> {
        if (event.succeeded()) {
          Set<InfectedHost> collected = event.result()
            .bodyAsJsonArray()
            .stream()
            .map(each -> (String) each)
            .map(InfectedHost::new)
            .collect(Collectors.toSet());
          result.complete(collected);
        } else {
          log.error("Unable to gossip with " + hostToGossipWith, event.cause());
          result.fail(event.cause());
        }
      });
    return result;
  }
}
