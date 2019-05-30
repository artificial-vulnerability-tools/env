package io.github.avt.env;

import io.github.avt.env.spreading.GossipClient;
import io.github.avt.env.spreading.InfectionClient;
import io.github.avt.env.spreading.impl.GossipClientImpl;
import io.github.avt.env.spreading.impl.InfectionClientImpl;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class Base {

  private static final Logger log = LoggerFactory.getLogger(Base.class);
  public final Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
  public final InfectionClient infectionClient = new InfectionClientImpl(vertx);
  public final GossipClient gossipClient = new GossipClientImpl(vertx);
  public final WebClient webClient = WebClient.create(vertx);

  public void undeploy(int amountOfNodes, TestContext testContext, List<String> idsToUndeploy) {
    Async undeployed = testContext.async(amountOfNodes);
    idsToUndeploy.forEach(id -> vertx.undeploy(id, done -> undeployed.countDown()));
    undeployed.await(30_000);
    log.info("Nodes had been undeployed");
  }


  public int jpsGrepAwkWC(TestContext testContext) {
    Process p;
    try {
      final String command = String.format("%s/bin/jps | grep AVTVirus | awk '{print $1}' | wc -l", System.getProperties().getProperty("java.home"));
      String[] cmd = {"/bin/bash", "-c", command};
      p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
      final int parseInt = Integer.parseInt(new BufferedReader(new InputStreamReader(p.getInputStream())).readLine().replaceAll("\\s+", ""));
      p.destroy();
      return parseInt;
    } catch (Exception e) {
      testContext.fail(e);
      return 0;
    }
  }
}
