package com.hubspot.mesos;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityContainerInfo {
  private final SingularityContainerType type;
  private final Optional<List<SingularityVolume>> volumes;
  private final Optional<SingularityDockerInfo> docker;
  private final Optional<SingularityMesosInfo> mesos;

  @JsonCreator
  public SingularityContainerInfo(
      @JsonProperty("type") SingularityContainerType type,
      @JsonProperty("volumes") Optional<List<SingularityVolume>> volumes,
      @JsonProperty("docker") Optional<SingularityDockerInfo> docker,
      @JsonProperty("mesos") Optional<SingularityMesosInfo> mesos) {
    this.type = type;
    this.volumes = volumes;
    this.docker = docker;
    this.mesos = mesos;
  }

  public SingularityContainerInfo(
      SingularityContainerType type,
      Optional<List<SingularityVolume>> volumes,
      Optional<SingularityDockerInfo> docker) {
    this(type, volumes, docker, Optional.absent());
  }

  @ApiModelProperty(required=true, value="Container type, can be MESOS or DOCKER. Default is MESOS")
  public SingularityContainerType getType() {
    return type;
  }

  @ApiModelProperty(required=false, value="List of volumes to mount. Applicable only to DOCKER container type")
  public Optional<List<SingularityVolume>> getVolumes() {
    return volumes;
  }

  @ApiModelProperty(required=false, value="Information specific to docker runtime settings")
  public Optional<SingularityDockerInfo> getDocker() {
    return docker;
  }

  @ApiModelProperty(required=false, value="Information specific to mesos containerizer")
  public Optional<SingularityMesosInfo> getMesos() {
    return mesos;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityContainerInfo that = (SingularityContainerInfo) o;
    return type == that.type &&
        Objects.equals(volumes, that.volumes) &&
        Objects.equals(docker, that.docker) &&
        Objects.equals(mesos, that.mesos);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, volumes, docker, mesos);
  }

  @Override
  public String toString() {
    return "SingularityContainerInfo{" +
        "type=" + type +
        ", volumes=" + volumes +
        ", docker=" + docker +
        ", mesos=" + mesos +
        '}';
  }
}
