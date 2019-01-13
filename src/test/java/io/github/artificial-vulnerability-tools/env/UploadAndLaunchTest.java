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
import com.github.avt.env.spreading.HostWithEnvironment;
import com.github.avt.env.spreading.InfectionClient;
import com.github.avt.env.spreading.impl.InfectionClientImpl;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.github.avt.env.Commons.LOCALHOST;
import static com.github.avt.env.TestLauncher.TEST_FILE_NAME;
import static com.github.avt.env.daemon.AVTService.AVT_HOME_DIR;

@RunWith(VertxUnitRunner.class)
public class UploadAndLaunchTest {

  public static final Logger log = LoggerFactory.getLogger(UploadAndLaunchTest.class);
  public InfectionClient infectionClient = new InfectionClientImpl();

  @Test
  public void uploadAndRun(TestContext testContext) {
    Vertx vertx = Vertx.vertx();
    Async async = testContext.async(2);
    AtomicReference<List<String>> startTestDirs = new AtomicReference<>(null);
    vertx.deployVerticle(new AVTService(), deployed -> {
      String deploymentId = deployed.result();
      Objects.requireNonNull(deploymentId);
      startTestDirs.set(vertx.fileSystem().readDirBlocking(AVT_HOME_DIR));
      infectionClient.infect(
        new HostWithEnvironment(LOCALHOST, AVTService.DEFAULT_PORT),
        Commons.TEST_FILE_WITH_VIRUS)
        .setHandler(ar -> {
          if (ar.succeeded()) {
            log.info("Virus topology service on port " + ar.result().topologyServicePort());
            vertx.setPeriodic(100, timerId -> {
              List<String> currentFiles = vertx.fileSystem().readDirBlocking(AVT_HOME_DIR);
              currentFiles.removeAll(startTestDirs.get());
              if (currentFiles.size() == 1) {
                List<String> files = vertx.fileSystem()
                  .readDirBlocking(currentFiles.get(0))
                  .stream().map(File::new)
                  .map(File::getName)
                  .collect(Collectors.toList());

                if (files.contains(TEST_FILE_NAME + "." + AVTService.DEFAULT_PORT)) {
                  vertx.cancelTimer(timerId);
                  vertx.undeploy(deploymentId, undeployed -> {
                    async.countDown();
                  });
                }
              }
            });
            async.countDown();
          } else {
            log.error("Unable to infect", ar.cause());
          }
        });
    });

    vertx.setTimer(10_0000, event -> {
      List<String> currentDirs = vertx.fileSystem().readDirBlocking(AVT_HOME_DIR);
      currentDirs.removeAll(startTestDirs.get());
      printLogFiles(vertx, currentDirs.get(0));
      testContext.fail();
    });
  }

  private void printLogFiles(Vertx vertx, String currentDir) {
    var dirWithLogs = currentDir + FileSystems.getDefault().getSeparator() + "logs";
    log.info("Dir with log file: " + dirWithLogs);
    vertx.fileSystem()
      .readDirBlocking(dirWithLogs)
      .stream()
      .map(File::new)
      .filter(file -> file.getName().endsWith("log.txt"))
      .forEach(file -> {
        try {
          Optional<String> reduced = Files.readAllLines(file.toPath()).stream().reduce((one, another) -> one + "\n\t" + another);
          log.info("File content of " + file.getName() + "\n" + reduced.orElse("EMPTY"));
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
  }
}
