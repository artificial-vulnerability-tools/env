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

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class InfectionService extends AbstractVerticle {

  public static final String INFECTION_ADDRESS = "infection";

  private static final Logger log = LoggerFactory.getLogger(InfectionService.class);

  @Override
  public void start() {
    vertx.eventBus().consumer(INFECTION_ADDRESS, event -> {
      var fileName = (String) event.body();
      log.info("Jar file to analyze:" + fileName);
      try {
        JarFile jarFile = new JarFile(fileName);
        Enumeration<JarEntry> e = jarFile.entries();

        URL[] urls = {new URL("jar:file:" + fileName + "!/")};
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
            log.info("class " + aClass.getName() + " is loaded. Super class is: " + superclass.getName());

          } catch (Throwable exp) {
            log.error("Not able to load: " + className);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}
