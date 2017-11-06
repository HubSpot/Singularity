package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosVolumeSource {
  private final Optional<MesosVolumeSourceType> type;
  private final Optional<MesosSandboxPath> sandboxPath;
  private final Optional<MesosDockerVolume> dockerVolume;

  @JsonCreator
  public MesosVolumeSource(@JsonProperty("type") Optional<MesosVolumeSourceType> type,
                           @JsonProperty("sandboxPath") Optional<MesosSandboxPath> sandboxPath,
                           @JsonProperty("dockerVolume") Optional<MesosDockerVolume> dockerVolume) {
    this.type = type;
    this.sandboxPath = sandboxPath;
    this.dockerVolume = dockerVolume;
  }

  public MesosVolumeSourceType getType() {
    return type.orNull();
  }

  public MesosSandboxPath getSandboxPath() {
    return sandboxPath.orNull();
  }

  public boolean hasSandboxPath() {
    return sandboxPath.isPresent();
  }

  public MesosDockerVolume getDockerVolume() {
    return dockerVolume.orNull();
  }

  public boolean hasDockerVolume() {
    return dockerVolume.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosVolumeSource) {
      final MesosVolumeSource that = (MesosVolumeSource) obj;
      return Objects.equals(this.type, that.type) &&
          Objects.equals(this.sandboxPath, that.sandboxPath) &&
          Objects.equals(this.dockerVolume, that.dockerVolume);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, sandboxPath, dockerVolume);
  }

  @Override
  public String toString() {
    return "MesosVolumeSource{" +
        "type=" + type +
        ", sandboxPath=" + sandboxPath +
        ", dockerVolume=" + dockerVolume +
        '}';
  }
}
