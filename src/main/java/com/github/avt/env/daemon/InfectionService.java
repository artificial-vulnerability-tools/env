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

package com.github.avt.env.daemon;

import com.github.avt.env.extend.Launcher;
import com.github.avt.env.process.ProcessMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.CopyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.github.avt.env.daemon.AVTService.SEPARATOR;


/**
 * Main idea of the service is to obtain a path to uploaded jar file and run the virus inside.
 */
public class InfectionService extends AbstractVerticle {

  public static final String INFECTION_ADDRESS = "infection";
  public static final String VIRUS_SCRIPT_NAME = "run_virus.sh";
  public static final String BASH_INTERPRETER_PATH = "/bin/bash";

  private static final Logger log = LoggerFactory.getLogger(InfectionService.class);

  private MessageConsumer consumer;
  private final ProcessMap currentProcesses = new ProcessMap();
  private final Integer avtServicePort;

  public InfectionService(Integer avtServicePort) {
    this.avtServicePort = avtServicePort;
  }

  @Override
  public void start() {
    consumer = vertx.eventBus().consumer(INFECTION_ADDRESS + ":" + avtServicePort, event -> {
      var obtainedJarFile = new File((String) event.body());
      log.info("Jar file to analyze:" + obtainedJarFile);
      try {
        JarFile jarFile = new JarFile(obtainedJarFile);
        Enumeration<JarEntry> e = jarFile.entries();

        URL[] urls = {new URL("jar:file:" + jarFile + "!/")};
        URLClassLoader cl = URLClassLoader.newInstance(urls);

        while (e.hasMoreElements()) {
          JarEntry je = e.nextElement();
          if (je.isDirectory() || !je.getName().endsWith(".class")) {
            continue;
          }
          // -6 because of .class
          String className = je.getName().substring(0, je.getName().length() - 6);
          className = className.replace('/', '.');
          try {
            Class<?> aClass = cl.loadClass(className);
            Class<?> superclass = aClass.getSuperclass();
            if (superclass.getName().equals(Launcher.class.getName())) {
              log.info("Class to run " + aClass);
              runVirus(obtainedJarFile, aClass.getName());
            }
          } catch (Throwable exp) {
            // not able to load a class
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    log.info("Successfully started");
  }

  @Override
  public void stop() {
    consumer.unregister();
    while (!currentProcesses.isEmpty()) {
      currentProcesses.forEach((pid, processHandle) -> {
        boolean result = processHandle.destroyForcibly();
        if (result) {
          log.info("process " + pid + " has been destroyed in a forcible manner");
          currentProcesses.remove(pid);
        } else {
          log.error("Unable to stop process " + pid);
        }
      });
    }
  }

  private void runVirus(File jar, String className) {
    try {
      File bashScriptGen = new File(
        Objects.requireNonNull(
          Thread.currentThread().getContextClassLoader().getResource(VIRUS_SCRIPT_NAME)
        ).toURI()
      );
      File parentDir = jar.getParentFile();
      String originalBashScriptPath = bashScriptGen.getAbsolutePath();
      String copyScriptPath = parentDir.getAbsolutePath() + SEPARATOR + VIRUS_SCRIPT_NAME;
      vertx.fileSystem().copy(originalBashScriptPath, copyScriptPath, new CopyOptions().setCopyAttributes(true), copyDone -> {
        if (copyDone.succeeded()) {
          log.info("Virus runner copied successfully");
          runVirusScript(new File(copyScriptPath), className);
        } else {
          log.error(String.format("Copy from %s to %s failed", originalBashScriptPath, parentDir.getAbsolutePath()), copyDone.cause());
        }
      });

    } catch (URISyntaxException e) {
      log.error("Unable to lookup bash script 'run_virus.sh' path", e);
    }
  }

  private void runVirusScript(File bashFile, String className) {
    try {
      List<String> args = new ArrayList<>();
      args.add(BASH_INTERPRETER_PATH);
      args.add(bashFile.getAbsolutePath());
      args.add(className);
      args.add(avtServicePort.toString());
      log.info("Spawning a new process:" + args.stream().reduce("", (s1, s2) -> s1 + " " + s2));
      ProcessBuilder pb = new ProcessBuilder(args);
      setupPathVariable(pb);
      logPathVariable(pb);
      startProcess(bashFile, pb);
    } catch (Exception e) {
      log.error("A problem with virus running occurred", e);
    }
  }

  private void startProcess(File bashFile, ProcessBuilder pb) throws IOException {
    pb.directory(bashFile.getParentFile());
    Process p = pb.start();
    ProcessHandle processHandle = p.toHandle();
    currentProcesses.put(processHandle.pid(), processHandle);
  }

  private void logPathVariable(ProcessBuilder pb) {
    String envString = pb.environment()
      .entrySet()
      .stream()
      .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
      .reduce("\n", (acc, newLine) -> acc + "\n" + newLine);
    log.debug("Process env: " + envString);
  }

  private void setupPathVariable(ProcessBuilder pb) {
    String path = pb.environment().get("PATH");
    String libPath = System.getProperties().getProperty("java.home");
    path = path + File.pathSeparator + libPath + "/bin"; // should actually include null checks
    pb.environment().put("PATH", path);
  }
}
