package io.github.avt.env.spreading.topology.raft;

public class HeartbeatResult {

  private final Long heartBeatSuccess;
  private final Long total;

  public HeartbeatResult(Long heartBeatSuccess, Long total) {
    this.heartBeatSuccess = heartBeatSuccess;
    this.total = total;
  }

  public Long getHeartBeatSuccess() {
    return heartBeatSuccess;
  }

  public Long getTotal() {
    return total;
  }

  @Override
  public String toString() {
    return "HeartbeatResult{" +
      "heartBeatSuccess=" + heartBeatSuccess +
      ", total=" + total +
      '}';
  }

  public boolean isQuorum() {
    if (heartBeatSuccess.equals(total)) {
      return true;
    }
    return Long.valueOf(heartBeatSuccess + 1).doubleValue() / Long.valueOf(total + 1).doubleValue() > 0.5d;
  }
}
