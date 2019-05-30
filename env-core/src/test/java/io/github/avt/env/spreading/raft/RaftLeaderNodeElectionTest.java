package io.github.avt.env.spreading.raft;

import io.github.avt.env.Base;
import io.github.avt.env.Commons;
import io.github.avt.env.daemon.AVTService;
import io.github.avt.env.spreading.meta.HostWithEnvironment;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.github.avt.env.spreading.topology.raft.RaftCentralizedTopology;
import io.github.avt.env.spreading.topology.raft.RaftStateName;
import io.github.avt.env.util.Utils;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(VertxUnitRunner.class)
public class RaftLeaderNodeElectionTest extends Base {

  private static final Logger log = LoggerFactory.getLogger(RaftLeaderNodeElectionTest.class);

  @Test(timeout = 60 * 1000 * 20)
  public void simple3NodesTest(TestContext testContext) throws InterruptedException {
    testContext.assertTrue(Commons.TEST_FILE_WITH_RAFT_VIRUS.exists(), "Test file with a virus should exists");
    int n = 3;
    int leadersConfirmations = 5;
    runLeaderTestOnNodesCount(testContext, n, leadersConfirmations);
  }

  @Test(timeout = 60 * 1000 * 20)
  public void simple5NodesTest(TestContext testContext) throws InterruptedException {
    testContext.assertTrue(Commons.TEST_FILE_WITH_RAFT_VIRUS.exists(), "Test file with a virus should exists");
    int n = 5;
    int leadersConfirmations = 5;
    runLeaderTestOnNodesCount(testContext, n, leadersConfirmations);
  }

  @Test(timeout = 60 * 1000 * 20)
  public void simple9NodesTest(TestContext testContext) throws InterruptedException {
    testContext.assertTrue(Commons.TEST_FILE_WITH_RAFT_VIRUS.exists(), "Test file with a virus should exists");
    int n = 9;
    int leadersConfirmations = 5;
    runLeaderTestOnNodesCount(testContext, n, leadersConfirmations);
  }

  private void runLeaderTestOnNodesCount(TestContext testContext, int n, int leadersConfirmations) {
    var idsToUndeploy = Collections.synchronizedList(new LinkedList<String>());
    final Async deployment = testContext.async(n);
    final List<AVTService> services = IntStream.rangeClosed(1, n).map(operand -> Utils.pickRandomFreePort()).mapToObj(value -> {
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
    deployment.await(n * 5_000);
    final Set<InfectedHost> infectedHostsWithoutInfection = services.stream().map(avtService -> new InfectedHost(new HostWithEnvironment(Commons.LOCALHOST, avtService.envPort()), InfectedHost.NOT_INFECTED)).collect(Collectors.toSet());
    Async oneNodeInfected = testContext.async();
    final AVTService avtService = services.get(0);
    infectionClient.infect(
      new HostWithEnvironment(Commons.LOCALHOST, avtService.envPort()),
      Commons.TEST_FILE_WITH_RAFT_VIRUS)
      .setHandler(event -> {
        if (event.succeeded()) {
          gossipClient.gossipWith(event.result(), infectedHostsWithoutInfection);
          oneNodeInfected.countDown();
        } else {
          testContext.fail("Unable to infect one of the nodes");
        }
      });
    oneNodeInfected.await(30_000);


    Set<InfectedHost> infectedHosts;
    do {
      infectedHosts = Collections.synchronizedSet(new HashSet<>());
      final Async topologyPorts = testContext.async(infectedHostsWithoutInfection.size());
      Set<InfectedHost> finalInfectedHosts = infectedHosts;
      infectedHostsWithoutInfection.stream()
        .map(host ->
          infectionClient.topologyServicePort(host.getHostWithEnv()).map(integer -> new InfectedHost(host.getHostWithEnv(), integer.orElse(0)))).forEach(infectedHostFuture -> {
        infectedHostFuture.setHandler(ev -> {
          if (ev.succeeded()) {
            finalInfectedHosts.add(ev.result());
            topologyPorts.countDown();
          } else {
            testContext.fail();
          }
        });
      });
      topologyPorts.await();
    } while (infectedHosts.stream().anyMatch(host -> host.topologyServicePort() == 0));

    log.info("All nodes are infected");

    Set<InfectedHost> finalInfectedHosts1 = infectedHosts;
    final Async singleLeader = testContext.async(leadersConfirmations);
    vertx.setPeriodic(((n / 10) + 1) * 1000, event -> {
      vertx.executeBlocking(e -> {
        AtomicInteger leaders = new AtomicInteger(0);
        final Async statusResponses = testContext.async(finalInfectedHosts1.size());
        finalInfectedHosts1.forEach(host -> {
          webClient.getAbs(String.format("http://%s:%d%s", Commons.LOCALHOST, host.topologyServicePort(), RaftCentralizedTopology.RAFT_STATE)).send(raftStateResp -> {
            if (raftStateResp.succeeded()) {
              final String state = raftStateResp.result().bodyAsString();
              log.info("State: '{}'. Host: '{}'", state, host.toString());
              if (state.trim().equals(RaftStateName.LEADER.name())) {
                leaders.incrementAndGet();
              }
            } else {
              log.error("status request failed", raftStateResp.failed());
            }
            statusResponses.countDown();
          });
        });
        statusResponses.await();
        log.info("Leaders {}", leaders.get());
        if (leaders.get() == 1 && singleLeader.count() != 0) {
          singleLeader.countDown();
        }
        e.complete();
      }, e -> {
      });
    });
    singleLeader.await(5_000 * leadersConfirmations);
    undeploy(n, testContext, idsToUndeploy);
  }
}
