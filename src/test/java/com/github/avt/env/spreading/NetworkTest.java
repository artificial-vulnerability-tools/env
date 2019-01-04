package com.github.avt.env.spreading;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@RunWith(VertxUnitRunner.class)
public class NetworkTest {

  private static final String HTTP_GOOGLE_COM = "http://www.google.com";
  private static final Logger log = LoggerFactory.getLogger(NetworkTest.class);

  @Test
  public void internetAddressLookupTest(TestContext testContext) {
    if (isInternetAvailable()) {
      Async async = testContext.async();
      log.info("Google is reachable. We are able to run the test");
      Network network = new Network(Network.NetworkType.IPv4_INTERNET, Vertx.vertx());
      network.getHostAddressInTheNetwork().setHandler(event -> {
        if (event.succeeded()) {
          testContext.assertEquals(internetIpAddress(), event.result());
          async.countDown();
        } else {
          testContext.fail();
        }
      });
    } else {
      log.info("Google is not reachable. We are not able to run the test");
    }
  }

  @Test
  public void internetAddressLookupBlockingTest(TestContext testContext) {
    if (isInternetAvailable()) {
      log.info("Google is reachable. We are able to run the test");
      Network network = new Network(Network.NetworkType.IPv4_INTERNET, Vertx.vertx());
      String address = network.getHostAddressInTheNetworkBlocking();
      testContext.assertEquals(internetIpAddress(), address);
    } else {
      log.info("Google is not reachable. We are not able to run the test");
    }
  }

  private static String internetIpAddress() {
    try {
      URL whatismyip = new URL("http://checkip.amazonaws.com");
      BufferedReader in = new BufferedReader(new InputStreamReader(
        whatismyip.openStream()));
      return in.readLine();
    } catch (Exception e) {
      throw new RuntimeException("Failed to obtain the internet IP address");
    }
  }

  private static boolean isInternetAvailable() {
    try {
      final URL url = new URL(HTTP_GOOGLE_COM);
      final URLConnection conn = url.openConnection();
      conn.connect();
      conn.getInputStream().close();
      return true;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      return false;
    }
  }
}
