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

package com.github.avt.env;

import com.github.avt.env.extend.Launcher;
import io.vertx.core.Vertx;

public class TestLauncher extends Launcher {

  public static final String TEST_FILE_NAME = "test-file.hello";

  @Override
  public void launch() {
    Vertx.vertx().fileSystem().createFileBlocking(TEST_FILE_NAME);
  }
}

