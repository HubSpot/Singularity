package com.hubspot.mesos.json;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class MesosResourcesObject {

  private final ImmutableMap<String, Object> properties;

  @JsonCreator
  public MesosResourcesObject(Map<String, Object> properties) {
    this.properties = ImmutableMap.copyOf(checkNotNull(properties, "properties is null"));
  }

  public Optional<Integer> getNumCpus() {
    return getResourceAsInteger("cpus");
  }
  
  public Optional<Integer> getNumGpus() {
    return getResourceAsInteger("gpus");
  }

  public Optional<Long> getDiskSpace() {
    return getResourceAsLong("disk");
  }

  public Optional<Integer> getMemoryMegaBytes() {
    return getResourceAsInteger("mem");
  }

  public Optional<String> getPorts() {
    return getResourceAsString("ports");
  }

  public Optional<Integer> getResourceAsInteger(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return properties.containsKey(resourceName) ? Optional.of(((Number) properties.get(resourceName)).intValue()) : Optional.<Integer> absent();
  }

  public Optional<Long> getResourceAsLong(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return properties.containsKey(resourceName) ? Optional.of(((Number) properties.get(resourceName)).longValue()) : Optional.<Long> absent();
  }

  public Optional<String> getResourceAsString(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return properties.containsKey(resourceName) ? Optional.of(properties.get(resourceName).toString()) : Optional.<String> absent();
  }

  public Optional<Object> getResourceAsObject(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return Optional.fromNullable(properties.get(resourceName));
  }
}
