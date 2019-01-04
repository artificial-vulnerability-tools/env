package com.github.avt.env.spreading;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

public class InfectedHost implements HasVertxSocketAddress {

  private final HostWithEnvironment hostWithEnv;
  private final int virusPort;

  public InfectedHost(HostWithEnvironment hostWithEnv, int virusPort) {
    this.hostWithEnv = hostWithEnv;
    this.virusPort = virusPort;
  }

  public HostWithEnvironment getHostWithEnv() {
    return hostWithEnv;
  }

  public int getVirusPort() {
    return virusPort;
  }

  @Override
  public SocketAddress toVertxSocketAddress() {
    return new SocketAddressImpl(virusPort, hostWithEnv.getHost());
  }
}
