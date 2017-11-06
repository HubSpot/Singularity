package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosVolume {
  private final Optional<String> containerPath;
  private final Optional<String> hostPath;
  private final Optional<MesosVolumeMode> mode;
  private final Optional<MesosVolumeSource> source;
  private final Optional<MesosImage> image;

  @JsonCreator

  public MesosVolume(@JsonProperty("containerPath") Optional<String> containerPath,
                     @JsonProperty("hostPath") Optional<String> hostPath,
                     @JsonProperty("mode") Optional<MesosVolumeMode> mode,
                     @JsonProperty("source") Optional<MesosVolumeSource> source,
                     @JsonProperty("image") Optional<MesosImage> image) {
    this.containerPath = containerPath;
    this.hostPath = hostPath;
    this.mode = mode;
    this.source = source;
    this.image = image;
  }

  public String getContainerPath() {
    return containerPath.orNull();
  }

  public boolean hasContainerPath() {
    return containerPath.isPresent();
  }

  public String getHostPath() {
    return hostPath.orNull();
  }

  public boolean hasHostPath() {
    return hostPath.isPresent();
  }

  public MesosVolumeMode getMode() {
    return mode.orNull();
  }

  public boolean hasMode() {
    return mode.isPresent();
  }

  public MesosVolumeSource getSource() {
    return source.orNull();
  }

  public boolean hasSource() {
    return source.isPresent();
  }

  public MesosImage getImage() {
    return image.orNull();
  }

  public boolean hasImage() {
    return image.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosVolume) {
      final MesosVolume that = (MesosVolume) obj;
      return Objects.equals(this.containerPath, that.containerPath) &&
          Objects.equals(this.hostPath, that.hostPath) &&
          Objects.equals(this.mode, that.mode) &&
          Objects.equals(this.source, that.source) &&
          Objects.equals(this.image, that.image);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(containerPath, hostPath, mode, source, image);
  }

  @Override
  public String toString() {
    return "MesosVolume{" +
        "containerPath=" + containerPath +
        ", hostPath=" + hostPath +
        ", mode=" + mode +
        ", source=" + source +
        ", image=" + image +
        '}';
  }
}
