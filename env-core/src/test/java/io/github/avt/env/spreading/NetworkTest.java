package io.github.avt.env.spreading;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.regex.Pattern;

@RunWith(VertxUnitRunner.class)
public class NetworkTest {

  private static final String HTTP_GOOGLE_COM = "http://www.google.com";
  private static final Logger log = LoggerFactory.getLogger(NetworkTest.class);

  @Test
  public void internetAddressLookupBlockingTest(TestContext testContext) {
    if (isInternetAvailable()) {
      log.info("Google is reachable. We are able to run the test");
      Network network = new Network(Network.NetworkType.IPv4_INTERNET, Vertx.vertx());
      String address = network.getHostAddressInTheNetworkBlocking();
      Objects.requireNonNull(address);
      testContext.assertTrue(validateAddr(address));
    } else {
      log.info("Google is not reachable. We are not able to run the test");
    }
  }

  private static final Pattern IP_v4_ADRESS_PATTERN = Pattern.compile(
    "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

  private boolean validateAddr(final String ip) {
    return IP_v4_ADRESS_PATTERN.matcher(ip).matches();
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
