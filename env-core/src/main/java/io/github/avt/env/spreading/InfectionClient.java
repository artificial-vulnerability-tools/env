package io.github.avt.env.spreading;

import io.vertx.core.Future;

import java.io.File;
import java.util.Optional;

public interface InfectionClient {

  Future<Optional<Integer>> topologyServicePort(HostWithEnvironment hostWithEnvironment);

  Future<InfectedHost> infect(HostWithEnvironment hostWithEnvironment);

  Future<InfectedHost> infect(HostWithEnvironment hostWithEnvironment, File artifactWithVirus);
}
