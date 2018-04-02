package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Beta
@Schema(description = "Describes a container image to be launched in mesos")
public class SingularityMesosImage {
  private final SingularityMesosImageType type;
  private final Optional<SingularityAppcImage> appc;
  private final Optional<SingularityDockerImage> docker;
  private final boolean cached;

  @JsonCreator
  public SingularityMesosImage(@JsonProperty("type") SingularityMesosImageType type,
                               @JsonProperty("appc") Optional<SingularityAppcImage> appc,
                               @JsonProperty("docker") Optional<SingularityDockerImage> docker,
                               @JsonProperty("cached") Boolean cached) {
    this.type = type;
    this.appc = appc;
    this.docker = docker;
    this.cached = MoreObjects.firstNonNull(cached, true);
  }

  @Schema(required = true, description = "Mesos image type")
  public SingularityMesosImageType getType() {
    return type;
  }

  @Schema(description = "Appc image configuration")
  public Optional<SingularityAppcImage> getAppc()
  {
    return appc;
  }

  @Schema(description = "Docker image configuration")
  public Optional<SingularityDockerImage> getDocker()
  {
    return docker;
  }

  @Schema(description = "Determines if a cached image is considered up to date")
  public boolean isCached()
  {
    return cached;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityMesosImage that = (SingularityMesosImage) o;
    return Objects.equals(type, that.type) &&
        Objects.equals(appc, that.appc) &&
        Objects.equals(docker, that.docker) &&
        Objects.equals(cached, that.cached);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, appc, docker, cached);
  }

  @Override
  public String toString() {
    return "SingularityMesosImage{" +
        "type='" + type + '\'' +
        "appc='" + appc + '\'' +
        "docker='" + docker + '\'' +
        "cached='" + cached + '\'' +
        '}';
  }
}
