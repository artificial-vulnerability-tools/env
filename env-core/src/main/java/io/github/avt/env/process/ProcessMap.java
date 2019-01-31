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

package io.github.avt.env.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Map, which is able to store alive processes.
 */
public class ProcessMap extends ConcurrentHashMap<Long, ProcessHandle> {

  private static final Logger log = LoggerFactory.getLogger(ProcessMap.class);

  @Override
  public ProcessHandle put(Long key, ProcessHandle value) {
    ProcessHandle put = super.put(key, value);
    CompletableFuture<ProcessHandle> processHandleCompletableFuture = value.onExit();
    processHandleCompletableFuture.thenRun(() -> {
      log.info("process " + key + " exit");
      remove(key);
    });
    return put;
  }

  @Override
  public ProcessHandle get(Object key) {
    ProcessHandle processHandle = super.get(key);
    if (processHandle != null && !processHandle.isAlive()) {
      this.remove(key);
      return null;
    }
    return processHandle;
  }
}
