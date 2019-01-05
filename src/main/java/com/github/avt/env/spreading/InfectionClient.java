package com.github.avt.env.spreading;

import io.vertx.core.Future;

import java.io.File;

public interface InfectionClient {

  Future<Void> infect(HostWithEnvironment hostWithEnvironment);

  Future<Void> infect(HostWithEnvironment hostWithEnvironment, File artifactWithVirus);
}
