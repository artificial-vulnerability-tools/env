package io.github.avt.env.spreading.p2p;

import io.github.avt.env.Base;
import io.github.avt.env.Commons;
import io.github.avt.env.daemon.AVTService;
import io.github.avt.env.spreading.meta.HostWithEnvironment;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.util.Utils;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(VertxUnitRunner.class)
public class HugeAmountOfNodesSpreading extends Base {

  public static final Logger log = LoggerFactory.getLogger(TwoLocalNodesTest.class);

  @Test(timeout = 60 * 1000 * 20)
  public void test100Nodes(TestContext testContext) throws InterruptedException {
    testContext.assertTrue(Commons.TEST_FILE_WITH_P2P_VIRUS.exists(), "Test file with a virus should exists");
    final int amountOfNodes = 10;
    log.info("Packaged virus file: '{}'", Commons.TEST_FILE_WITH_P2P_VIRUS.getAbsolutePath());
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
      Commons.TEST_FILE_WITH_P2P_VIRUS)
      .setHandler(event -> {
        if (event.succeeded()) {
          gossipClient.gossipWith(event.result(), infectedHostsWithoutInfection);
          oneNodeInfected.countDown();
        } else {
          testContext.fail("Unable to infect one of the nodes");
        }
      });
    oneNodeInfected.await(30_000);
    final Async allVirusesAreRunning = testContext.async(5);
    vertx.setPeriodic(1000, event -> {
      final int virusProcesses = jpsGrepAwkWC(testContext);
      log.info("Current amount of virus processes='{}'", virusProcesses);
      if (virusProcesses == amountOfNodes) {
        allVirusesAreRunning.countDown();
        if (allVirusesAreRunning.count() == 0) {
          vertx.cancelTimer(event);
        }
      }
    });
    allVirusesAreRunning.await();
    undeploy(amountOfNodes, testContext, idsToUndeploy);
  }
}
