package com.github.avt.env.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

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
}
