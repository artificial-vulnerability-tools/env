package io.github.avt.env.daemon;

import java.io.File;

public class Virus {

  private final ProcessHandle process;
  private final File uploadDir;
  private final Integer topologyServicePort;

  public Virus(ProcessHandle process, File uploadDir, Integer topologyServicePort) {
    this.process = process;
    this.uploadDir = uploadDir;
    this.topologyServicePort = topologyServicePort;
  }

  public boolean isAlive() {
    return process.isAlive();
  }

  public Long pid() {
    return process.pid();
  }

  public File getUploadDir() {
    return uploadDir;
  }

  public Integer getTopologyServicePort() {
    return topologyServicePort;
  }

  public ProcessHandle getProcess() {
    return process;
  }
}
