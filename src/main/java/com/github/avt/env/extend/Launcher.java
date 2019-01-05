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

package com.github.avt.env.extend;

import com.github.avt.env.spreading.Network;
import com.github.avt.env.spreading.Topology;
import com.github.avt.env.spreading.impl.PeerToPeerNetworkTopology;

/**
 * A base class for extending. Should be used by any virus.
 */
public abstract class Launcher {

  /**
   * You should override this method. Virus code should be executed inside this method.
   */
  public abstract void launch(int envPort);

  /**
   * Related to the way how the virus spreads across the environment and overlay network topology.
   */
  public Topology topology =
    new PeerToPeerNetworkTopology(
      PeerToPeerNetworkTopology.PEER_TO_PEER_TOPOLOGY_DEFAULT_PORT,
      new Network(Network.NetworkType.IPv4_INTERNET)
    );

  public void start(int envPort) {
    topology.runTopologyService(envPort);
    launch(envPort);
  }
}
