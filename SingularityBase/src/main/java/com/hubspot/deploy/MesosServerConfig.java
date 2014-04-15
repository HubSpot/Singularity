package com.hubspot.deploy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

@JsonIgnoreProperties(ignoreUnknown=true)
public class MesosServerConfig {
  public static final boolean RACK_SENSITIVE_DEFAULT = true;

  private final Optional<Integer> instances;

  private final Optional<Integer> cpus;

  private final Optional<Integer> memory;

  private final Boolean rackSensitive;

  @JsonCreator
  public MesosServerConfig(@JsonProperty("instances") Integer instances,
                           @JsonProperty("numCpus") Integer cpus,
                           @JsonProperty("memoryMb") Integer memory,
                           @JsonProperty("rackSensitive") Boolean rackSensitive) {
    this.instances = Optional.fromNullable(instances);
    this.cpus = Optional.fromNullable(cpus);
    this.memory = Optional.fromNullable(memory);
    this.rackSensitive = rackSensitive != null ? rackSensitive : RACK_SENSITIVE_DEFAULT;
  }

  public Optional<Integer> getInstances() {
    return instances;
  }

  public Optional<Integer> getCpus() {
    return cpus;
  }

  public Optional<Integer> getMemory() {
    return memory;
  }

  public Boolean isRackSensitive() {
    return rackSensitive;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("instances", instances)
        .add("cpus", cpus)
        .add("memory", memory)
        .add("rackSensitive", rackSensitive)
        .toString();
  }
}
