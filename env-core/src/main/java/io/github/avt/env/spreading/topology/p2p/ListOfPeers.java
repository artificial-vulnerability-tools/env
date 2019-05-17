package io.github.avt.env.spreading.topology.p2p;

import io.github.avt.env.spreading.TopologyInformation;
import io.github.avt.env.spreading.meta.InfectedHost;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.avt.env.spreading.meta.InfectedHost.NOT_INFECTED;

public class ListOfPeers implements TopologyInformation {

  private static final Logger log = LoggerFactory.getLogger(ListOfPeers.class);

  private Vertx vertx;
  private final InfectedHost thisPeer;
  private final Map<String, InfectedHost> peers = new ConcurrentHashMap<>();
  private Handler<InfectedHost> addHandler;

  public ListOfPeers(Vertx vertx, InfectedHost thisPeer) {
    this(vertx, thisPeer, null);
  }

  public ListOfPeers(Vertx vertx, InfectedHost thisPeer, Handler<InfectedHost> addHandler) {
    this.vertx = vertx;
    this.thisPeer = thisPeer;
    peers.put(thisPeer.getHostWithEnv().toString(), thisPeer);
    this.addHandler = addHandler;
  }

  public synchronized boolean addPeer(InfectedHost peerToAdd) {
    final String newKey = peerToAdd.getHostWithEnv().toString();
    final InfectedHost infectedHost = peers.get(newKey);
    if (infectedHost != null) {
      if (infectedHost.topologyServicePort() == NOT_INFECTED && peerToAdd.topologyServicePort() != NOT_INFECTED) {
        peers.put(newKey, peerToAdd);
        log.info("Replacing '{}'->'{}'", infectedHost, peerToAdd);
        notifyNewElement(peerToAdd);
        return true;
      } else {
        return false;
      }
    } else {
      peers.put(newKey, peerToAdd);
      notifyNewElement(peerToAdd);
      return true;
    }
  }

  public synchronized Set<InfectedHost> currentPeers() {
    HashSet<InfectedHost> peersToReturn = new HashSet<>(peers.values());
    peersToReturn.remove(thisPeer);
    return peersToReturn;
  }

  public synchronized Set<InfectedHost> fullPeerList() {
    return new HashSet<>(peers.values());
  }

  public synchronized void removePeer(InfectedHost peerToRemove) {
    peers.remove(peerToRemove.getHostWithEnv().toString());
  }

  public synchronized void newPeerHandler(Handler<InfectedHost> handler) {
    this.addHandler = handler;
  }

  public void notifyNewElement(InfectedHost peer) {
    if (addHandler != null && !peer.equals(thisPeer) && peer.topologyServicePort() != NOT_INFECTED) {
      vertx.getOrCreateContext().runOnContext(event -> {
        addHandler.handle(peer);
      });
    }
  }

}
