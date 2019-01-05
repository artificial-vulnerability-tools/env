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

package com.github.avt.env.daemon;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.avt.env.daemon.InfectionService.INFECTION_ADDRESS;

/**
 * Artificial vulnerability services. Aimed to expose an endpoint for a virus.
 */
public class AVTService extends AbstractVerticle {

  private final Logger log;
  public static final int DEFAULT_PORT = 2222;
  public static final String AVT_HOME_DIR = ".avtenv";
  public static final String NAME_OF_JAR_WITH_VIRUS = "virus.jar";
  public static final String INFECT_PATH = "/infect";
  public static final String SEPARATOR = System.getProperty("file.separator");

  private final Integer actualPort;

  public AVTService() {
    this(DEFAULT_PORT);
  }

  public AVTService(Integer port) {
    actualPort = port;
    log = LoggerFactory.getLogger(AVTService.class + ":" + actualPort);
  }

  @Override
  public void start(Future<Void> startFuture) {
    var dirExist = vertx.fileSystem().existsBlocking(AVT_HOME_DIR);
    if (!dirExist) {
      vertx.fileSystem().mkdirBlocking(AVT_HOME_DIR);
    }
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.post(INFECT_PATH).handler(routingContext -> {
      routingContext.request().bodyHandler(body -> {
        var dirName = dirName();
        var dirNameToCreate = AVT_HOME_DIR + SEPARATOR + dirName;
        var jarFileName = dirNameToCreate + SEPARATOR + NAME_OF_JAR_WITH_VIRUS;
        vertx.fileSystem().mkdir(dirNameToCreate, dirCreated -> {
          if (dirCreated.succeeded()) {
            vertx.fileSystem().writeFile(jarFileName, body, done -> {
              vertx.eventBus().send(INFECTION_ADDRESS + ":" + actualPort, jarFileName);
              routingContext.response().end("DONE");
            });
          }
        });
      });
    });
    Future<String> infectionVerticleDeployed = Future.future();
    vertx.deployVerticle(new InfectionService(actualPort), infectionVerticleDeployed);
    infectionVerticleDeployed.compose(id -> {
      Future<HttpServer> listenFuture = Future.future();
      server.requestHandler(router).listen(actualPort, listenFuture);
      return listenFuture;
    }).setHandler(event -> {
      if (event.succeeded()) {
        log.info("AVTService successfully started");
        startFuture.complete();
      } else {
        log.error("Unable to start the AVTService", event.cause());
        startFuture.fail(event.cause());
      }
    });
  }

  private String dirName() {
    var dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss");
    var now = LocalDateTime.now();
    return "port_" + actualPort + "_time_" + dtf.format(now);
  }
}
