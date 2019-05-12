package io.github.avt.env.spreading;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.HashSet;
import java.util.Set;

public class ListOfPeers implements TopologyInformation {

  private Vertx vertx;
  private final InfectedHost thisPeer;
  private final Set<InfectedHost> peers = new ConcurrentHashSet<>();
  private Handler<InfectedHost> addHandler;

  public ListOfPeers(Vertx vertx, InfectedHost thisPeer) {
    this(vertx, thisPeer, null);
  }

  public ListOfPeers(Vertx vertx, InfectedHost thisPeer, Handler<InfectedHost> addHandler) {
    this.vertx = vertx;
    this.thisPeer = thisPeer;
    peers.add(thisPeer);
    this.addHandler = addHandler;
  }

  public synchronized boolean addPeer(InfectedHost peer) {
    boolean add = peers.add(peer);
    if (add && addHandler != null && !peer.equals(thisPeer)) {
      vertx.getOrCreateContext().runOnContext(event -> {
        addHandler.handle(peer);
      });
    }
    return add;
  }

  public Set<InfectedHost> currentPeers() {
    HashSet<InfectedHost> peersToReturn = new HashSet<>(peers);
    peersToReturn.remove(thisPeer);
    return peersToReturn;
  }

  public Set<InfectedHost> fullPeerList() {
    return new HashSet<>(peers);
  }


  public void removePeer(InfectedHost peerToRemove) {
    peers.remove(peerToRemove);
  }

  public synchronized void newPeerHandler(Handler<InfectedHost> handler) {
    this.addHandler = handler;
  }

}
