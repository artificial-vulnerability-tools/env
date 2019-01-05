package com.github.avt.env.spreading.impl;

import com.github.avt.env.spreading.HostWithEnvironment;
import com.github.avt.env.spreading.InfectionClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.github.avt.env.daemon.AVTService.INFECT_PATH;
import static com.github.avt.env.daemon.AVTService.NAME_OF_JAR_WITH_VIRUS;

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
  public Future<Void> infect(HostWithEnvironment hostWithEnvironment) {
    return infect(hostWithEnvironment, new File(NAME_OF_JAR_WITH_VIRUS));
  }

  @Override
  public Future<Void> infect(HostWithEnvironment hostWithEnvironment, File artifactWithVirus) {
    Future<Void> result = Future.future();
    vertx.fileSystem().open(artifactWithVirus.getAbsolutePath(), new OpenOptions(), fileRes -> {
      if (fileRes.succeeded()) {
        ReadStream<Buffer> fileStream = fileRes.result();
        webClient
          .post(hostWithEnvironment.getEnvPort(), hostWithEnvironment.getHost(), INFECT_PATH)
          .sendStream(fileStream, ar -> {
            if (ar.succeeded()) {
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
}
