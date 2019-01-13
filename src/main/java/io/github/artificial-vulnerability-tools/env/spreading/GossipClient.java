package com.github.avt.env.spreading;

import io.vertx.core.Future;

import java.util.Set;

public interface GossipClient {

  Future<Set<InfectedHost>> gossipWith(InfectedHost hostToGossipWith, Set<InfectedHost> info);
}
