package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosImage {
  private final Optional<MesosImageType> type;
  private final Optional<Boolean> cached;
  private final Optional<MesosDockerImage> docker;
  private final Optional<MesosAppcImage> appc;

  @JsonCreator

  public MesosImage(@JsonProperty("type") Optional<MesosImageType> type,
                    @JsonProperty("cached") Optional<Boolean> cached,
                    @JsonProperty("docker") Optional<MesosDockerImage> docker,
                    @JsonProperty("appc") Optional<MesosAppcImage> appc) {
    this.type = type;
    this.cached = cached;
    this.docker = docker;
    this.appc = appc;
  }

  public MesosImageType getType() {
    return type.orNull();
  }

  public boolean hasType() {
    return type.isPresent();
  }

  public Boolean getCached() {
    return cached.orNull();
  }

  public boolean hasCached() {
    return cached.isPresent();
  }

  public MesosDockerImage getDocker() {
    return docker.orNull();
  }

  public boolean hasDocker() {
    return docker.isPresent();
  }

  public MesosAppcImage getAppc() {
    return appc.orNull();
  }

  public boolean hasAppc() {
    return appc.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosImage) {
      final MesosImage that = (MesosImage) obj;
      return Objects.equals(this.type, that.type) &&
          Objects.equals(this.cached, that.cached) &&
          Objects.equals(this.docker, that.docker) &&
          Objects.equals(this.appc, that.appc);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, cached, docker, appc);
  }

  @Override
  public String toString() {
    return "MesosImage{" +
        "type=" + type +
        ", cached=" + cached +
        ", docker=" + docker +
        ", appc=" + appc +
        '}';
  }
}
