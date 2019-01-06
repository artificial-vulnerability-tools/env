package com.github.avt.env.spreading;

import com.github.avt.env.Commons;
import com.github.avt.env.daemon.AVTService;
import com.github.avt.env.spreading.impl.GossipClientImpl;
import com.github.avt.env.spreading.impl.InfectionClientImpl;
import com.github.avt.env.util.Utils;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
  private final String LOCALHOST = "localhost";

  @Test
  public void infectionShouldSpread(TestContext testContext) throws IOException, InterruptedException {
    var idsToUndelpoy = Collections.synchronizedList(new LinkedList<String>());
    var env1 = new AVTService(FIRST_ENV_NODE_PORT);
    var env2 = new AVTService(SECOND_ENV_NODE_PORT);
    Async async = testContext.async(2);
    List.of(env1, env2).forEach(each -> {
      vertx.deployVerticle(each, event -> {
        if (event.succeeded()) {
          idsToUndelpoy.add(event.result());
          async.countDown();
        } else {
          testContext.fail(event.cause());
        }
      });
    });
    async.await();
    log.info("2 AVT verticles have been deployed");
    Async oneNodeInfected = testContext.async();
    if (Commons.TEST_FILE_WITH_VIRUS.exists()) {
      log.info("Uploading file with virus at " + Commons.TEST_FILE_WITH_VIRUS.getCanonicalPath());
    } else {
      testContext.fail("Unable to fina file with virus");
    }
    infectionClient.infect(
      new HostWithEnvironment(LOCALHOST, FIRST_ENV_NODE_PORT),
      Commons.TEST_FILE_WITH_VIRUS)
      .setHandler(event -> {
        if (event.succeeded()) {
          InfectedHost infectedHost = new InfectedHost(new HostWithEnvironment(LOCALHOST, SECOND_ENV_NODE_PORT), Utils.pickRandomFreePort());
          log.info("Forcing " + event.result() + " to infect " + infectedHost);
          gossipClient.gossipWith(event.result(), Set.of(infectedHost));
          oneNodeInfected.countDown();
        } else {
          testContext.fail("Unable to infect one of the nodes");
        }
      });


    oneNodeInfected.await(20_000);
    log.info("One of the nodes has been infected");
    Async undeployed = testContext.async(2);
    vertx.setPeriodic(100, timerId -> {
      if (true) {
        vertx.cancelTimer(timerId);
        idsToUndelpoy.forEach(id -> {
          vertx.undeploy(id, done -> {
            undeployed.countDown();
          });
        });
      }
    });
    undeployed.await(20_000);
    log.info("Nodes has been undeployed");
  }
}
