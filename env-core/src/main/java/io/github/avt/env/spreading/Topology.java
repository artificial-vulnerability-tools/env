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

package io.github.avt.env.spreading;

/**
 * Defines the way a virus can spread across the environment.
 */
public interface Topology<T extends TopologyInformation> {

  /**
   * @return a information about topology
   */
  T topologyInformation();

  /**
   * Launches the service, related to a specific network topology.
   *
   * @param envPort the env port
   */
  void runTopologyService(int envPort);

  int topologyServicePort();
}
