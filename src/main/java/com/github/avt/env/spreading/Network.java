package com.github.avt.env.spreading;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Network {

  private final NetworkType networkType;
  private final Vertx vertx;
  private final WebClient webClient;
  private static final String INTERNET_V4_LOOKUP_SITE = "http://checkip.amazonaws.com";
  private static final Logger log = LoggerFactory.getLogger(Network.class);

  public Network(NetworkType networkType, Vertx vertx) {
    this.networkType = networkType;
    this.vertx = vertx;
    this.webClient = WebClient.create(vertx);
  }

  public Network(NetworkType networkType) {
    this(networkType, Vertx.vertx());
  }

  public Future<String> getHostAddressInTheNetwork() {
    if (networkType.equals(NetworkType.LOCAL)) {
      return Future.succeededFuture("127.0.0.1");
    } else if (networkType.equals(NetworkType.IPv4_INTERNET)) {
      Future<String> ipFuture = Future.future();
      webClient.getAbs(INTERNET_V4_LOOKUP_SITE).send(response -> {
        if (response.succeeded()) {
          String result = response.result().bodyAsString().trim();
          log.debug("INTERNET IP is " + result);
          ipFuture.complete(result);
        } else {
          ipFuture.fail("Unable to reach " + INTERNET_V4_LOOKUP_SITE);
        }
      });
      return ipFuture;
    } else {
      return Future.failedFuture(new IllegalStateException("Unknown Network type"));
    }
  }


  public String getHostAddressInTheNetworkBlocking() {
    AtomicReference<String> result = new AtomicReference<>(null);
    CountDownLatch countDownLatch = new CountDownLatch(1);
    getHostAddressInTheNetwork().setHandler(event -> {
      if (event.succeeded()) {
        countDownLatch.countDown();
        result.set(event.result());
      } else {
        log.error("Not able to reach " + INTERNET_V4_LOOKUP_SITE, event.cause());
      }
    });

    try {
      countDownLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("CDL await issue", e);
    }
    return result.get();
  }

  public NetworkType getNetworkType() {
    return networkType;
  }

  public enum NetworkType {
    LOCAL,
    IPv4_INTERNET
  }
}
