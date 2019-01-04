package com.github.avt.env.spreading;

import io.vertx.core.Future;

public interface InfectionClient {

  Future<Void> infect(HostWithEnvironment hostWithEnvironment);
}
