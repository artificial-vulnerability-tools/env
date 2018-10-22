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
import io.vertx.ext.web.Router;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.github.avt.env.daemon.InfectionService.INFECTION_ADDRESS;

public class AVTService extends AbstractVerticle {

  public static final int PORT = 2222;
  public static final String DIR = ".avtenv";
  public static final String SEPARATOR = System.getProperty("file.separator");

  private boolean isInfected = false;

  @Override
  public void start() {
    var dirExist = vertx.fileSystem().existsBlocking(DIR);
    if (!dirExist) {
      vertx.fileSystem().mkdirBlocking(DIR);
    }
    var server = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.post("/infect").handler(routingContext -> {
      routingContext.request().bodyHandler(body -> {
        var dirName = dirName();
        var dirNameToCreate = DIR + SEPARATOR + dirName;
        var jarFileName = dirNameToCreate + SEPARATOR + "uploaded.jar";
        vertx.fileSystem().mkdir(dirNameToCreate, dirCreated -> {
          if (dirCreated.succeeded()) {
            vertx.fileSystem().writeFile(jarFileName, body, done -> {
              vertx.eventBus().send(INFECTION_ADDRESS, jarFileName);
              routingContext.response().end("DONE");
            });
          }
        });
      });
    });

    vertx.deployVerticle(new InfectionService());
    server.requestHandler(router::accept).listen(PORT);
  }

  private String dirName() {
    var dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss");
    var now = LocalDateTime.now();
    return dtf.format(now);
  }
}
