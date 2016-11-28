package com.hubspot.mesos;

import java.util.List;

import org.apache.mesos.Protos.ContainerInfo.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityContainerInfo {
  private final SingularityContainerType type;
  private final Optional<List<SingularityVolume>> volumes;
  private final Optional<SingularityDockerInfo> docker;

  @JsonCreator
  public SingularityContainerInfo(
      @JsonProperty("type") SingularityContainerType type,
      @JsonProperty("volumes") Optional<List<SingularityVolume>> volumes,
      @JsonProperty("docker") Optional<SingularityDockerInfo> docker) {
    this.type = type;
    this.volumes = volumes;
    this.docker = docker;
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

  @Override
  public String toString() {
    return String.format("ContainerInfo [type=%s, volumes=%s, docker=%s]", type, volumes, docker);
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

    if (!type.equals(that.type)) {
      return false;
    }
    if (!volumes.equals(that.volumes)) {
      return false;
    }
    if (!docker.equals(that.docker)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + volumes.hashCode();
    result = 31 *result + docker.hashCode();
    return result;
  }
}
