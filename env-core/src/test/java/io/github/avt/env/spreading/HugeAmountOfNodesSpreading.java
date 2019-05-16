package io.github.avt.env.spreading;

import io.github.avt.env.Commons;
import io.github.avt.env.daemon.AVTService;
import io.github.avt.env.spreading.impl.GossipClientImpl;
import io.github.avt.env.spreading.impl.InfectionClientImpl;
import io.github.avt.env.util.Utils;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(VertxUnitRunner.class)
public class HugeAmountOfNodesSpreading {

  private final Vertx vertx = Vertx.vertx();
  public static final Logger log = LoggerFactory.getLogger(TwoLocalNodesTest.class);
  public final InfectionClient infectionClient = new InfectionClientImpl(vertx);
  public final GossipClient gossipClient = new GossipClientImpl(vertx);

  @Test(timeout = 60 * 1000 * 20 )
  public void test100Nodes(TestContext testContext) throws InterruptedException {
    testContext.assertTrue(Commons.TEST_FILE_WITH_VIRUS.exists(), "Test file with a virus should exists");
    final int amountOfNodes = 10;
    log.info("Packaged virus file: '{}'", Commons.TEST_FILE_WITH_VIRUS.getAbsolutePath());
    checkAmountOfVirusesNoMoreThan(amountOfNodes, testContext);
    runP2PNetworkTest(amountOfNodes, testContext);
  }

  void runP2PNetworkTest(int amountOfNodes, TestContext testContext) throws InterruptedException {
    var idsToUndeploy = Collections.synchronizedList(new LinkedList<String>());
    final Async deployment = testContext.async(amountOfNodes);
    final List<AVTService> services = IntStream.rangeClosed(1, amountOfNodes).map(operand -> Utils.pickRandomFreePort()).mapToObj(value -> {
      final AVTService avtService = new AVTService(value);
      vertx.deployVerticle(avtService, event -> {
        if (event.succeeded()) {
          idsToUndeploy.add(event.result());
          deployment.countDown();
        } else {
          log.error("Unable to deploy AVT service");
          testContext.fail(event.cause());
        }
      });
      return avtService;
    }).collect(Collectors.toList());
    final Set<InfectedHost> infectedHostsWithoutInfection = services.stream().map(avtService -> new InfectedHost(new HostWithEnvironment(Commons.LOCALHOST, avtService.envPort()), InfectedHost.NOT_INFECTED)).collect(Collectors.toSet());
    deployment.await();
    log.info("Nodes had been deployed");
    final AVTService avtService = services.get(0);
    Async oneNodeInfected = testContext.async();
    infectionClient.infect(
      new HostWithEnvironment(Commons.LOCALHOST, avtService.envPort()),
      Commons.TEST_FILE_WITH_VIRUS)
      .setHandler(event -> {
        if (event.succeeded()) {
          gossipClient.gossipWith(event.result(), infectedHostsWithoutInfection);
          oneNodeInfected.countDown();
        } else {
          testContext.fail("Unable to infect one of the nodes");
        }
      });
    final Async allVirusesAreRunning = testContext.async(5);
    vertx.setPeriodic(1000, event -> {
      final int virusProcesses = jpsGrepAwkWC(testContext);
      if (virusProcesses == amountOfNodes) {
        allVirusesAreRunning.countDown();
        if (allVirusesAreRunning.count() == 0) {
          vertx.cancelTimer(event);
        }
      }
    });
    allVirusesAreRunning.await();
    oneNodeInfected.await(30_000);
    undeploy(amountOfNodes, testContext, idsToUndeploy);
  }

  private void undeploy(int amountOfNodes, TestContext testContext, List<String> idsToUndeploy) {
    Async undeployed = testContext.async(amountOfNodes);
    idsToUndeploy.forEach(id -> vertx.undeploy(id, done -> undeployed.countDown()));
    undeployed.await(30_000);
    log.info("Nodes had been undeployed");
  }

  private int jpsGrepAwkWC(TestContext testContext) {
    Process p;
    try {
      final String command = String.format("%s/bin/jps | grep AVTVirus | awk '{print $1}' | wc -l", System.getProperties().getProperty("java.home"));
      String[] cmd = {"/bin/bash", "-c", command};
      p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
      final int parseInt = Integer.parseInt(new BufferedReader(new InputStreamReader(p.getInputStream())).readLine());
      p.destroy();
      return parseInt;
    } catch (Exception e) {
      testContext.fail(e);
      return 0;
    }
  }

  private void checkAmountOfVirusesNoMoreThan(int count, TestContext context) {
    vertx.setPeriodic(1000, event -> {
      final int amountOfProcesses = jpsGrepAwkWC(context);
      log.info("Current amount of virus processes='{}'", amountOfProcesses);
      if (amountOfProcesses > count) {
        context.fail(String.format("Amount of virus processes'%d' more than expected '%d'", amountOfProcesses));
      }
    });
  }
}
