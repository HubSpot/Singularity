package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Beta
public class SingularityVolumeSource {
  private final SingularityVolumeSourceType type;
  private final Optional<SingularityDockerVolume> dockerVolume;

  @JsonCreator
  public SingularityVolumeSource(
      @JsonProperty("type") SingularityVolumeSourceType type,
      @JsonProperty("dockerVolume") Optional<SingularityDockerVolume> dockerVolume) {
    this.type = MoreObjects.firstNonNull(type, SingularityVolumeSourceType.UNKNOWN);
    this.dockerVolume = dockerVolume;
  }

  @ApiModelProperty(required=false, value="Volume source type")
  public SingularityVolumeSourceType getType() {
    return type;
  }

  @ApiModelProperty(required=false, value="Docker source volume spec")
  public Optional<SingularityDockerVolume> getDockerVolume() {
    return dockerVolume;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityVolumeSource that = (SingularityVolumeSource) o;
    return Objects.equals(type, that.type) &&
        Objects.equals(dockerVolume, that.dockerVolume);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, dockerVolume);
  }

  @Override
  public String toString() {
    return "SingularityVolumeSource{" +
        "type='" + type + '\'' +
        ", dockerVolume=" + dockerVolume +
        '}';
  }
}
