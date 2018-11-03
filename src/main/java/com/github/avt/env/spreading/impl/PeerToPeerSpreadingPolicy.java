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

package com.github.avt.env.spreading.impl;

import com.github.avt.env.spreading.Node;
import com.github.avt.env.spreading.SpreadingPolicy;

import java.util.HashSet;
import java.util.Set;

public class PeerToPeerSpreadingPolicy implements SpreadingPolicy {

  private Set<Node> peers = new HashSet<>();

  @Override
  public void spreadTo(Node node) {
    boolean addOperation = peers.add(node);
    // if the element is new
    if (addOperation) {

    }

    // if we already know the peer
    else {

    }
  }
}