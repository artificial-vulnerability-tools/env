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

  public InfectedHost(String uri) {
    hostWithEnv = new HostWithEnvironment(uri);
    var split = uri.split(":");
    if (split.length >= 3) {
      throw new IllegalStateException("Not able to parse InfectedHost from string. " +
        "Uri should have at least host, port and virus port");
    }
    virusPort = Integer.parseInt(split[2]);
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

  @Override
  public String toString() {
    return hostWithEnv.toString() + ":" + virusPort;
  }
}
