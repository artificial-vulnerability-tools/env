package io.github.avt.env.spreading.impl;

import io.github.avt.env.daemon.AVTService;
import io.github.avt.env.spreading.InfectionClient;
import io.github.avt.env.spreading.meta.HostWithEnvironment;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public class InfectionClientImpl implements InfectionClient {

  private static final Logger log = LoggerFactory.getLogger(InfectionClientImpl.class);
  private final Vertx vertx;
  private final WebClient webClient;

  public InfectionClientImpl(Vertx vertx) {
    this.vertx = vertx;
    this.webClient = WebClient.create(vertx);
  }

  public InfectionClientImpl() {
    this(Vertx.vertx());
  }

  @Override
  public Future<Optional<Integer>> topologyServicePort(HostWithEnvironment hostWithEnvironment) {
    Future<Optional<Integer>> result = Future.future();
    webClient
      .get(hostWithEnvironment.getEnvPort(), hostWithEnvironment.getHost(), AVTService.VIRUS_TOPOLOGY_ON_PORT)
      .send(event -> {
        if (event.succeeded()) {
          Integer toplogyServicePort = event.result().bodyAsJsonObject().getInteger(AVTService.TOPOLOGY_SERVICE_PORT);
          if (toplogyServicePort != 0) {
            result.complete(Optional.of(toplogyServicePort));
          } else {
            result.complete(Optional.empty());
          }
        } else {
          result.fail(event.cause());
        }
      });
    return result;
  }

  @Override
  public Future<InfectedHost> infect(HostWithEnvironment hostWithEnvironment) {
    return infect(hostWithEnvironment, new File(AVTService.NAME_OF_JAR_WITH_VIRUS));
  }

  @Override
  public Future<InfectedHost> infect(HostWithEnvironment hostWithEnvironment, File artifactWithVirus) {
    Future<InfectedHost> result = Future.future();
    log.info("Infecting '{}'", hostWithEnvironment);
    vertx.fileSystem().open(artifactWithVirus.getAbsolutePath(), new OpenOptions(), fileRes -> {
      if (fileRes.succeeded()) {
        ReadStream<Buffer> fileStream = fileRes.result();
        webClient
          .post(hostWithEnvironment.getEnvPort(), hostWithEnvironment.getHost(), AVTService.INFECT_PATH)
          .sendStream(fileStream, ar -> {
            if (ar.succeeded()) {
              HttpResponse<Buffer> response = ar.result();
              JsonObject entries = response.bodyAsJsonObject();
              log.info("Received on infection:'\n{}", entries.encodePrettily());
              if (entries.containsKey(AVTService.TOPOLOGY_SERVICE_PORT)) {
                Integer port = entries.getInteger(AVTService.TOPOLOGY_SERVICE_PORT);
                result.complete(new InfectedHost(hostWithEnvironment, port));
              } else {
                result.fail(entries.getString("error"));
              }
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
}
