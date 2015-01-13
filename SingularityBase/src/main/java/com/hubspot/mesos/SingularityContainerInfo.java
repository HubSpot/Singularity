package com.hubspot.mesos;

import java.util.List;

import org.apache.mesos.Protos.ContainerInfo.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityContainerInfo {
  private final Type type;
  private final Optional<List<SingularityVolume>> volumes;
  private final Optional<SingularityDockerInfo> docker;

  @JsonCreator
  public SingularityContainerInfo(
      @JsonProperty("type") Type type,
      @JsonProperty("volumes") Optional<List<SingularityVolume>> volumes,
      @JsonProperty("docker") Optional<SingularityDockerInfo> docker) {
    this.type = type;
    this.volumes = volumes;
    this.docker = docker;
  }

  public Type getType() {
    return type;
  }

  public Optional<List<SingularityVolume>> getVolumes() {
    return volumes;
  }

  public Optional<SingularityDockerInfo> getDocker() {
    return docker;
  }

  @Override
  public String toString() {
    return String.format("ContainerInfo [type=%s, volumes=%s, docker=%s]", type, volumes, docker);
  }
}
