package com.github.avt.env.spreading;

import io.vertx.core.Future;

import java.io.File;

public interface InfectionClient {

  Future<InfectedHost> infect(HostWithEnvironment hostWithEnvironment);

  Future<InfectedHost> infect(HostWithEnvironment hostWithEnvironment, File artifactWithVirus);
}
