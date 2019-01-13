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

package io.github.avt.env.daemon;

import io.github.avt.env.util.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Artificial vulnerability services. Aimed to expose an endpoint for a virus.
 */
public class AVTService extends AbstractVerticle {

  private final Logger log;
  public static final int DEFAULT_PORT = 2222;
  public static final String AVT_HOME_DIR = ".avtenv";
  public static final String NAME_OF_JAR_WITH_VIRUS = "virus.jar";
  public static final String INFECT_PATH = "/infect";
  public static final String VIRUS_TOPOLOGY_ON_PORT = "/infected";
  public static final String TOPOLOGY_SERVICE_PORT = "topology_service_port";
  public static final String SEPARATOR = System.getProperty("file.separator");

  private final ReactivePort reactivePort = new ReactivePort();
  private final Integer avtServicePort;
  private final AtomicInteger virusTopologyServicePort = new AtomicInteger(0);

  public AVTService() {
    this(DEFAULT_PORT);
  }

  public AVTService(Integer port) {
    avtServicePort = port;
    log = LoggerFactory.getLogger(AVTService.class + ":" + avtServicePort);
  }

  @Override
  public void start(Future<Void> startFuture) {
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.post(INFECT_PATH).handler(routingContext -> {
      log.info("Received upload request");
      routingContext.request().bodyHandler(body -> {
        log.info("Virus has been uploaded");
        var dirName = dirName();
        var dirNameToCreate = AVT_HOME_DIR + SEPARATOR + dirName;
        var jarFileName = dirNameToCreate + SEPARATOR + NAME_OF_JAR_WITH_VIRUS;
        vertx.fileSystem().mkdir(dirNameToCreate, dirCreation -> {
          if (dirCreation.succeeded()) {
            log.info("Directory " + dirNameToCreate + " has been created");
            vertx.fileSystem().writeFile(jarFileName, body, done -> {
              vertx.eventBus().send(InfectionService.INFECTION_ADDRESS + ":" + avtServicePort, jarFileName);
              reactivePort.whenInfected(port -> {
                virusTopologyServicePort.set(port);
                routingContext.response().end(Utils.virusPortJson(port).toBuffer());
              });
            });
          } else {
            log.error("Unable to create a directory for a virus", dirCreation.cause());
          }
        });
      });
    });
    router.post(VIRUS_TOPOLOGY_ON_PORT).handler(routingContext -> {
      routingContext.request().bodyHandler(body -> {
        Integer infectedPort = body.toJsonObject().getInteger(TOPOLOGY_SERVICE_PORT);
        log.info("Received INFECTED ack on port " + infectedPort);
        reactivePort.infectedPort(infectedPort);
        routingContext.response().end();
      });
    });
    router.get(VIRUS_TOPOLOGY_ON_PORT).handler(routingContext -> {
      routingContext.response().end(Utils.virusPortJson(virusTopologyServicePort.get()).toBuffer());
    });

    vertx.fileSystem().mkdir(AVT_HOME_DIR, done -> {
      if (done.succeeded()) {
        log.info(AVT_HOME_DIR + " dir has been created");
      } else {
        log.info(AVT_HOME_DIR + " already exists");
      }
      Future<String> infectionVerticleDeployed = Future.future();
      vertx.deployVerticle(new InfectionService(avtServicePort), infectionVerticleDeployed);
      infectionVerticleDeployed.compose(id -> {
        Future<HttpServer> listenFuture = Future.future();
        server.requestHandler(router).listen(avtServicePort, listenFuture);
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
    });
  }

  private String dirName() {
    var dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss_SSS");
    var now = LocalDateTime.now();
    return "port_" + avtServicePort + "_time_" + dtf.format(now);
  }
}
