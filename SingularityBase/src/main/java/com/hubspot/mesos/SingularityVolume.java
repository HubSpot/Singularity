package com.hubspot.mesos;

import org.apache.mesos.Protos;

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
      @JsonProperty("mode") SingularityDockerVolumeMode mode) {
    this.containerPath = containerPath;
    this.hostPath = hostPath;
    this.mode = Optional.fromNullable(mode);
  }

  @Deprecated
  public SingularityVolume(String containerPath, Optional<String> hostPath, Optional<Protos.Volume.Mode> mode) {
    this(containerPath, hostPath, convertedMode(mode));
  }

  private static SingularityDockerVolumeMode convertedMode(Optional<Protos.Volume.Mode> mode) {
    if (mode.isPresent()) {
      return SingularityDockerVolumeMode.valueOf(mode.get().toString());
    } else {
      return null;
    }
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
    return "SingularityVolume{" +
        "containerPath='" + containerPath + '\'' +
        ", hostPath=" + hostPath +
        ", mode=" + mode +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityVolume that = (SingularityVolume) o;

    if (!containerPath.equals(that.containerPath)) {
      return false;
    }
    if (!hostPath.equals(that.hostPath)) {
      return false;
    }
    if (!mode.equals(that.mode)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = containerPath.hashCode();
    result = 31 * result + hostPath.hashCode();
    result = 31 * result + mode.hashCode();
    return result;
  }
}
