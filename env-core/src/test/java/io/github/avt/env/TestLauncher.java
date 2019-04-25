/*
 *
 * Copyright 2018 Pavel Drankou.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.avt.env;

import io.github.avt.env.extend.Launcher;
import io.github.avt.env.spreading.ListOfPeers;
import io.github.avt.env.spreading.Network;
import io.github.avt.env.spreading.Topology;
import io.github.avt.env.spreading.TopologyInformation;
import io.github.avt.env.spreading.impl.PeerToPeerNetworkTopology;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TestLauncher extends Launcher {

  private static final Logger log = LoggerFactory.getLogger(TestLauncher.class);

  public static final String TEST_FILE_NAME = "test-file.hello";

  @Override
  public Topology topology() {
    return new PeerToPeerNetworkTopology(new Network(Network.NetworkType.LOCAL));
  }

  @Override
  public synchronized void launch(int envPort) {
    Vertx.vertx().fileSystem().createFileBlocking(TEST_FILE_NAME + "." + envPort);
    TopologyInformation topologyInformation = topology.topologyInformation();
    ListOfPeers peers = (ListOfPeers) topologyInformation;
    Objects.requireNonNull(peers);
    peers.newPeerHandler(peer -> {
      log.info("DEVELOPER SEE: New peer is {}", peer);
    });
  }
}

