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
