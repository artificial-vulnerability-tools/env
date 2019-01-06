package com.github.avt.env.spreading;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

import java.util.Objects;

public class HostWithEnvironment implements HasVertxSocketAddress {

  private final String host;
  private final Integer envPort;

  public HostWithEnvironment(String host, Integer envPort) {
    Objects.requireNonNull(host);
    Objects.requireNonNull(envPort);
    this.host = host;
    this.envPort = envPort;
  }

  public HostWithEnvironment(String uri) {
    Objects.requireNonNull(uri);
    var split = uri.split(":");
    if (split.length < 2) {
      throw new IllegalStateException(String.format("Uri[%s] should have at least host and port", uri));
    }
    host = split[0];
    envPort = Integer.parseInt(split[1]);
    Objects.requireNonNull(host);
    Objects.requireNonNull(envPort);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HostWithEnvironment)) return false;
    HostWithEnvironment that = (HostWithEnvironment) o;
    return getHost().equals(that.getHost()) &&
      getEnvPort().equals(that.getEnvPort());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getHost(), getEnvPort());
  }
}
