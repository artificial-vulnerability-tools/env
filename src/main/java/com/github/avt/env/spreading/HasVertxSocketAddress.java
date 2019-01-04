package com.github.avt.env.spreading;

import io.vertx.core.net.SocketAddress;

public interface HasVertxSocketAddress {

  SocketAddress toVertxSocketAddress();
}
