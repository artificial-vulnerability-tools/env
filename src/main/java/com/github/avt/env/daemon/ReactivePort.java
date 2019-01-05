package com.github.avt.env.daemon;

import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReactivePort {

  private final Queue<Future<Integer>> futuresToCompleteWithPort = new LinkedList<>();
  private final Queue<Integer> incomingPorts = new LinkedBlockingQueue<>();

  public synchronized void infectedPort(Integer infectedPort) {
    if (futuresToCompleteWithPort.isEmpty()) {
      incomingPorts.add(infectedPort);
    } else {
      futuresToCompleteWithPort.remove().complete(infectedPort);
    }
  }

  public synchronized void whenInfected(Handler<Integer> portHandler) {
    if (incomingPorts.isEmpty()) {
      Future<Integer> futureToComplete = Future.future();
      futuresToCompleteWithPort.add(futureToComplete);
      futureToComplete.setHandler(event -> portHandler.handle(event.result()));
    } else {
      portHandler.handle(incomingPorts.remove());
    }
  }
}
