package com.github.avt.env.spreading;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

public class HostWithEnvironment implements HasVertxSocketAddress {

  private final String host;
  private final Integer envPort;

  public HostWithEnvironment(String host, Integer envPort) {
    this.host = host;
    this.envPort = envPort;
  }

  public HostWithEnvironment(String uri) {
    var split = uri.split(":");
    if (split.length >= 2) {
      throw new IllegalStateException("uri should have at least host and port");
    }
    host = split[0];
    envPort = Integer.parseInt(split[1]);
  }

  public String getHost() {
    return host;
  }

  public Integer getEnvPort() {
    return envPort;
  }

  @Override
  public SocketAddress toVertxSocketAddress() {
    return new SocketAddressImpl(getEnvPort(), getHost());
  }

  @Override
  public String toString() {
    return host + ":" + envPort;
  }
}
