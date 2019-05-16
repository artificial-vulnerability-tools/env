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

import io.github.avt.env.spreading.InfectedHost;
import io.github.avt.env.util.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

  private final Integer avtServicePort;
  private final AtomicReference<Optional<Virus>> currentVirus = new AtomicReference<>(Optional.empty());
  private final ReactivePort reactivePort = new ReactivePort();
  private final AtomicBoolean canAcceptRequests = new AtomicBoolean(true);
  private InfectionHelper infectionHelper;

  public AVTService() {
    this(DEFAULT_PORT);
  }

  public AVTService(Integer port) {
    avtServicePort = port;
    log = LoggerFactory.getLogger(String.format("%s:%s", AVTService.class.getName(), avtServicePort));
  }

  public int envPort() {
    return avtServicePort;
  }

  @Override
  public void start(Future<Void> startFuture) {
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.post(INFECT_PATH).handler(routingContext -> {
      log.info("Received POST /infect request");
      if (currentVirus.get().isPresent()) {
        final String msg = "Attempt to infect had been rejected. Reason: there is a currently running virus";
        responseWithError(routingContext, msg);
      } else if (canAcceptRequests.compareAndSet(true, false)) {
        routingContext.request().bodyHandler(body -> {
          log.info("Virus has been uploaded");
          var dirName = dirName();
          var dirNameToCreate = AVT_HOME_DIR + SEPARATOR + dirName;
          var jarFileName = dirNameToCreate + SEPARATOR + NAME_OF_JAR_WITH_VIRUS;
          vertx.fileSystem().mkdir(dirNameToCreate, dirCreation -> {
            if (dirCreation.succeeded()) {
              log.info("Directory " + dirNameToCreate + " has been created");
              vertx.fileSystem().writeFile(jarFileName, body, done -> {
                final File obtainedJarFile = new File(jarFileName);
                vertx.<ProcessHandle>executeBlocking(event -> {
                  final Optional<ProcessHandle> processHandle = infectionHelper.processVirus(obtainedJarFile);
                  if (processHandle.isPresent()) {
                    event.complete(processHandle.get());
                  } else {
                    event.fail("A virus process has not created");
                  }
                }, event -> {
                  if (event.succeeded()) {
                    reactivePort.whenInfected(port -> {
                      final Virus virus = new Virus(event.result(), obtainedJarFile, port);
                      currentVirus.set(Optional.of(virus));
                      routingContext.response().end(Utils.virusPortJson(port).toBuffer());
                      log.info("infected, unlocking the infection lock");
                      canAcceptRequests.compareAndSet(false, true);
                    });
                  } else {
                    log.error("failed to start a process", event.cause());
                    canAcceptRequests.compareAndSet(false, true);
                  }
                });
              });
            } else {
              log.error("Unable to create a directory for a virus", dirCreation.cause());
              canAcceptRequests.compareAndSet(false, true);
            }
          });
        });

      } else {
        final String msg = "Attempt to infect had been rejected. Reason: unable to lock the infectionLock";
        responseWithError(routingContext, msg);
      }
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
      routingContext.response().end(Utils.virusPortJson(currentVirus.get().map(Virus::getTopologyServicePort).orElse(InfectedHost.NOT_INFECTED)).toBuffer());
    });

    vertx.fileSystem().mkdir(AVT_HOME_DIR, done -> {
      if (done.succeeded()) {
        log.info(AVT_HOME_DIR + " dir has been created");
      } else {
        log.info(AVT_HOME_DIR + " already exists");
      }
      infectionHelper = new InfectionHelper(avtServicePort);
      Future<HttpServer> listenFuture = Future.future();
      server.requestHandler(router).listen(avtServicePort, listenFuture);
      listenFuture.setHandler(event -> {
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

  private void responseWithError(RoutingContext routingContext, String msg) {
    routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code())
      .end(Buffer.buffer(new JsonObject().put("error", msg).encodePrettily().getBytes(StandardCharsets.UTF_8)));
  }

  private String dirName() {
    var dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss_SSS");
    var now = LocalDateTime.now();
    return "port_" + avtServicePort + "_time_" + dtf.format(now);
  }
}
