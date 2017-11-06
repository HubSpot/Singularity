package com.hubspot.mesos.protos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosDiskInfo {
  private final Optional<MesosDiskSource> source;
  private final Optional<MesosVolume> volume;
  private final Optional<MesosDiskPersistence> persistence;

  @JsonCreator
  public MesosDiskInfo(@JsonProperty("source") Optional<MesosDiskSource> source,
                       @JsonProperty("volume") Optional<MesosVolume> volume,
                       @JsonProperty("persistence") Optional<MesosDiskPersistence> persistence) {
    this.source = source;
    this.volume = volume;
    this.persistence = persistence;
  }

  public MesosDiskSource getSource() {
    return source.orNull();
  }

  public boolean hasSource() {
    return source.isPresent();
  }

  public MesosVolume getVolume() {
    return volume.orNull();
  }

  public boolean hasVolume() {
    return volume.isPresent();
  }

  public MesosDiskPersistence getPersistence() {
    return persistence.orNull();
  }

  public boolean hasPersistence() {
    return persistence.isPresent();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosDiskInfo) {
      final MesosDiskInfo that = (MesosDiskInfo) obj;
      return Objects.equals(this.source, that.source) &&
          Objects.equals(this.volume, that.volume) &&
          Objects.equals(this.persistence, that.persistence);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, volume, persistence);
  }

  @Override
  public String toString() {
    return "MesosDiskInfo{" +
        "source=" + source +
        ", volume=" + volume +
        ", persistence=" + persistence +
        '}';
  }
}
