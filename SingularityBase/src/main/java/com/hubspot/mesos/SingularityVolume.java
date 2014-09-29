package com.hubspot.mesos;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.mesos.Protos.Volume.Mode;

public class SingularityVolume {
  private final String containerPath;
  private final Optional<String> hostPath;
  private final Mode mode;

  @JsonCreator
  public SingularityVolume(
      @JsonProperty("containerPath") String containerPath,
      @JsonProperty("hostPath") Optional<String> hostPath,
      @JsonProperty("mode") Mode mode) {
    this.containerPath = containerPath;
    this.hostPath = hostPath;
    this.mode = mode;
  }

  public String getContainerPath() {
    return containerPath;
  }

  public Optional<String> getHostPath() {
    return hostPath;
  }

  public Mode getMode() {
    return mode;
  }

  @Override
  public String toString() {
    return String.format("Volume [containerPath=%s, hostPath=%s, mode=%s]", containerPath, hostPath, mode);
  }
}
