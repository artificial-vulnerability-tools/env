package io.github.avt.env.spreading.p2p;

import io.github.avt.env.Commons;
import io.github.avt.env.daemon.AVTService;
import io.github.avt.env.spreading.GossipClient;
import io.github.avt.env.spreading.InfectionClient;
import io.github.avt.env.spreading.impl.GossipClientImpl;
import io.github.avt.env.spreading.impl.InfectionClientImpl;
import io.github.avt.env.spreading.meta.HostWithEnvironment;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.vertx.core.Vertx;
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

@RunWith(VertxUnitRunner.class)
public class TwoLocalNodesTest {

  private final Vertx vertx = Vertx.vertx();
  public static final Logger log = LoggerFactory.getLogger(TwoLocalNodesTest.class);
  public final InfectionClient infectionClient = new InfectionClientImpl(vertx);
  public final GossipClient gossipClient = new GossipClientImpl(vertx);

  private final Integer FIRST_ENV_NODE_PORT = 2222;
  private final Integer SECOND_ENV_NODE_PORT = 2223;
  private final HostWithEnvironment FIRST_HOST_WITH_ENV = new HostWithEnvironment(Commons.LOCALHOST, FIRST_ENV_NODE_PORT);
  private final HostWithEnvironment SECOND_HOST_WITH_ENV = new HostWithEnvironment(Commons.LOCALHOST, SECOND_ENV_NODE_PORT);

  @Test
  public void infectionShouldSpread(TestContext testContext) {
    testContext.assertTrue(Commons.TEST_FILE_WITH_P2P_VIRUS.exists(), "Test file with a virus should exists");
    log.info("Packaged virus file: '{}'", Commons.TEST_FILE_WITH_P2P_VIRUS.getAbsolutePath());
    var idsToUndeploy = Collections.synchronizedList(new LinkedList<String>());
    var env1 = new AVTService(FIRST_ENV_NODE_PORT);
    var env2 = new AVTService(SECOND_ENV_NODE_PORT);
    Async async = testContext.async(2);
    List.of(env1, env2).forEach(each -> {
      vertx.deployVerticle(each, event -> {
        if (event.succeeded()) {
          idsToUndeploy.add(event.result());
          async.countDown();
        } else {
          testContext.fail(event.cause());
        }
      });
    });
    async.await();
    log.info("2 AVT verticles have been deployed");
    Async oneNodeInfected = testContext.async();
    infectionClient.infect(
      new HostWithEnvironment(Commons.LOCALHOST, FIRST_ENV_NODE_PORT),
      Commons.TEST_FILE_WITH_P2P_VIRUS)
      .setHandler(event -> {
        if (event.succeeded()) {
          InfectedHost infectedHost = new InfectedHost(new HostWithEnvironment(Commons.LOCALHOST, SECOND_ENV_NODE_PORT), InfectedHost.NOT_INFECTED);
          log.info("Forcing " + event.result() + " to infect " + SECOND_HOST_WITH_ENV);
          gossipClient.gossipWith(event.result(), Set.of(infectedHost));
          oneNodeInfected.countDown();
        } else {
          testContext.fail("Unable to infect one of the nodes");
        }
      });
    oneNodeInfected.await(30_000);
    log.info("One of the nodes has been infected");
    Async undeployed = testContext.async(2);
    vertx.setPeriodic(500, timerId -> {
      infectionClient.topologyServicePort(SECOND_HOST_WITH_ENV).setHandler(event -> {
        if (event.succeeded() && event.result().isPresent()) {
          gossipClient.gossipWith(new InfectedHost(SECOND_HOST_WITH_ENV, event.result().get()), Set.of()).setHandler(gossipResult -> {
            if (gossipResult.succeeded()) {
              Set<InfectedHost> result = gossipResult.result();
              result.forEach(each -> {
                if (each.getHostWithEnv().equals(FIRST_HOST_WITH_ENV)) {
                  vertx.cancelTimer(timerId);
                  idsToUndeploy.forEach(id -> vertx.undeploy(id, done -> undeployed.countDown()));
                }
              });
            }
          });
        }
      });
    });
    undeployed.await(30_000);
    log.info("Nodes has been undeployed");
  }
}
