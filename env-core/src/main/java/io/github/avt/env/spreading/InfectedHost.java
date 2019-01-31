package io.github.avt.env.spreading;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

import java.util.Objects;

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
    if (split.length < 3) {
      throw new IllegalStateException(String.format("Not able to parse InfectedHost from string. " +
        "Uri[%s] should have at least host, port and virus port", uri));
    }
    virusPort = Integer.parseInt(split[2]);
  }

  public HostWithEnvironment getHostWithEnv() {
    return hostWithEnv;
  }

  public int topologyServicePort() {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InfectedHost)) return false;
    InfectedHost that = (InfectedHost) o;
    return topologyServicePort() == that.topologyServicePort() &&
      getHostWithEnv().equals(that.getHostWithEnv());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getHostWithEnv(), topologyServicePort());
  }
}
