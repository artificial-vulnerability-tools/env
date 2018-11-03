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

package com.github.avt.env.extend;

import com.github.avt.env.spreading.SpreadingPolicy;
import com.github.avt.env.spreading.impl.DefaultNode;
import com.github.avt.env.spreading.impl.PeerToPeerSpreadingPolicy;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * A base class for extending. Should be used by any virus.
 */
public abstract class Launcher {

  /**
   * You should override this method. Virus code should be executed inside this method.
   */
  public abstract void launch();

  /**
   * Related to the way how the virus spreads across the environment.
   */
  public SpreadingPolicy spreadingPolicy = new PeerToPeerSpreadingPolicy();

  private Vertx vertx = Vertx.vertx();

  public static final Integer VIRUS_PORT = 2223;

  public void start() {
    var httpServer = vertx.createHttpServer();
    var router = Router.router(vertx);
    router.post("spread-to").handler(ctx -> {
      ctx.request().bodyHandler(body -> {
        JsonObject json = body.toJsonObject();
        String uri = json.getString("uri");
        String[] split = uri.split(":");
        spreadingPolicy.spreadTo(new DefaultNode(split[0], Integer.parseInt(split[1])));
      });
    });
    httpServer.requestHandler(router::accept).listen(VIRUS_PORT);
    launch();
  }
}
