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

import com.github.avt.env.daemon.AVTService;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.avt.env.TestLauncher.TEST_FILE_NAME;
import static com.github.avt.env.daemon.AVTService.DIR;

@RunWith(VertxUnitRunner.class)
public class UploadAndLaunchTest {

  public static final Logger log = LoggerFactory.getLogger(UploadAndLaunchTest.class);

  @Test
  public void uploadAndRun(TestContext testContext) {
    Vertx vertx = Vertx.vertx();
    Async async = testContext.async(2);
    vertx.deployVerticle(new AVTService(), deployed -> {
      List<String> startTestDirs = vertx.fileSystem().readDirBlocking(DIR);
      WebClient webClient = WebClient.create(vertx);
      vertx.fileSystem().open("build/libs/env-test-fat.jar", new OpenOptions(), fileRes -> {
        if (fileRes.succeeded()) {
          ReadStream<Buffer> fileStream = fileRes.result();
          webClient
            .post(AVTService.PORT, "localhost", "/infect")
            .sendStream(fileStream, ar -> {
              if (ar.succeeded()) {
                log.info("Jar uploaded");
                async.countDown();
              }
            });
        }
      });

      vertx.setPeriodic(100, timerId -> {
        List<String> currentFiles = vertx.fileSystem().readDirBlocking(DIR);
        currentFiles.removeAll(startTestDirs);
        if (currentFiles.size() == 1) {
          List<String> files = vertx.fileSystem()
            .readDirBlocking(currentFiles.get(0))
            .stream().map(File::new)
            .map(File::getName)
            .collect(Collectors.toList());

          if (files.contains(TEST_FILE_NAME)) {
            printLogFile(vertx, currentFiles.get(0));
            vertx.cancelTimer(timerId);
            async.countDown();
          }
        }
      });
    });
  }

  private void printLogFile(Vertx vertx, String currentDir) {
    File logTxtFile = vertx.fileSystem()
      .readDirBlocking(currentDir)
      .stream()
      .map(File::new)
      .filter(file -> file.getName().endsWith("log.txt"))
      .findAny()
      .get();
    try {
      Files.readAllLines(logTxtFile.toPath()).forEach(line -> {
        log.info("VIRUS: " + line);
      });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
