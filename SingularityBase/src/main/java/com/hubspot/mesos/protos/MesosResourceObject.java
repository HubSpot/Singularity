package com.hubspot.mesos.protos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosResourceObject {
  private final Optional<String> name;
  private final Optional<MesosDiskInfo> disk;
  private final Optional<MesosDoubleValue> scalar;
  private final Optional<MesosRanges> ranges;
  private final Optional<MesosSet> set;
  private final Optional<MesosReservationInfo> reservation;
  private final Optional<Boolean> revocable;
  private final Optional<String> role;
  private final Optional<Boolean> shared;
  private final Optional<MesosAttributeType> type;

  @JsonCreator

  public MesosResourceObject(@JsonProperty("name") Optional<String> name,
                             @JsonProperty("disk") Optional<MesosDiskInfo> disk,
                             @JsonProperty("scalar") Optional<MesosDoubleValue> scalar,
                             @JsonProperty("ranges") Optional<MesosRanges> ranges,
                             @JsonProperty("set") Optional<MesosSet> set,
                             @JsonProperty("reservation") Optional<MesosReservationInfo> reservation,
                             @JsonProperty("revocable") Optional<Boolean> revocable,
                             @JsonProperty("role") Optional<String> role,
                             @JsonProperty("shared") Optional<Boolean> shared,
                             @JsonProperty("type") Optional<MesosAttributeType> type) {
    this.name = name;
    this.disk = disk;
    this.scalar = scalar;
    this.ranges = ranges;
    this.set = set;
    this.reservation = reservation;
    this.revocable = revocable;
    this.role = role;
    this.shared = shared;
    this.type = type;
  }

  public String getName() {
    return name.orNull();
  }

  @JsonIgnore
  public boolean hasName() {
    return name.isPresent();
  }

  public MesosDiskInfo getDisk() {
    return disk.orNull();
  }

  @JsonIgnore
  public boolean hasDisk() {
    return disk.isPresent();
  }

  public MesosDoubleValue getScalar() {
    return scalar.orNull();
  }

  @JsonIgnore
  public boolean hasScalar() {
    return scalar.isPresent();
  }

  public MesosRanges getRanges() {
    return ranges.orNull();
  }

  @JsonIgnore
  public boolean hasRanges() {
    return ranges.isPresent();
  }

  public MesosSet getSet() {
    return set.orNull();
  }

  @JsonIgnore
  public boolean hasSet() {
    return set.isPresent();
  }

  public MesosReservationInfo getReservation() {
    return reservation.orNull();
  }

  @JsonIgnore
  public boolean hasReservation() {
    return reservation.isPresent();
  }

  public Boolean getRevocable() {
    return revocable.orNull();
  }

  @JsonIgnore
  public boolean hasRevocable() {
    return revocable.isPresent();
  }

  public String getRole() {
    return role.orNull();
  }

  @JsonIgnore
  public boolean hasRole() {
    return role.isPresent();
  }

  public Boolean getShared() {
    return shared.orNull();
  }

  @JsonIgnore
  public boolean hasShared() {
    return shared.isPresent();
  }

  public MesosAttributeType getType() {
    return type.orNull();
  }

  @JsonIgnore
  public boolean hasType() {
    return type.isPresent();
  }
}
