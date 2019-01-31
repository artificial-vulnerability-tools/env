package io.github.avt.env.util;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

import static io.github.avt.env.daemon.AVTService.TOPOLOGY_SERVICE_PORT;

public class Utils {

  private static final Logger log = LoggerFactory.getLogger(Utils.class);

  public static int pickRandomFreePort() {
    try {
      ServerSocket serverSocket = new ServerSocket(0);
      int localPort = serverSocket.getLocalPort();
      serverSocket.close();
      return localPort;
    } catch (IOException e) {
      log.error("An exception during picking random port selection process occured", e);
      return -1;
    }
  }

  public static JsonObject virusPortJson(int port) {
    return new JsonObject().put(TOPOLOGY_SERVICE_PORT, port);
  }
}
