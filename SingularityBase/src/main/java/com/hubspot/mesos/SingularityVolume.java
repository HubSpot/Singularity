package com.hubspot.mesos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityVolume {
  private final String containerPath;
  private final Optional<String> hostPath;
  private final Optional<SingularityDockerVolumeMode> mode;

  @JsonCreator
  public SingularityVolume(
      @JsonProperty("containerPath") String containerPath,
      @JsonProperty("hostPath") Optional<String> hostPath,
      @JsonProperty("mode") Optional<SingularityDockerVolumeMode> mode) {
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

  public Optional<SingularityDockerVolumeMode> getMode() {
    return mode;
  }

  @Override
  public String toString() {
    return String.format("Volume [containerPath=%s, hostPath=%s, mode=%s]", containerPath, hostPath, mode);
  }
}
