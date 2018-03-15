package com.hubspot.mesos;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class MesosResourcesObject {

  private final ImmutableMap<String, Object> properties;

  @JsonCreator
  public MesosResourcesObject(Map<String, Object> properties) {
    this.properties = ImmutableMap.copyOf(checkNotNull(properties, "properties is null"));
  }

  @JsonIgnore
  public Optional<Integer> getNumCpus() {
    return getResourceAsInteger("cpus");
  }

  @JsonIgnore
  public Optional<Long> getDiskSpace() {
    return getResourceAsLong("disk");
  }

  @JsonIgnore
  public Optional<Integer> getMemoryMegaBytes() {
    return getResourceAsInteger("mem");
  }

  @JsonIgnore
  public Optional<String> getPorts() {
    return getResourceAsString("ports");
  }

  public Optional<Integer> getResourceAsInteger(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return properties.containsKey(resourceName) ? Optional.of(((Number) properties.get(resourceName)).intValue()) : Optional.empty();
  }

  public Optional<Long> getResourceAsLong(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return properties.containsKey(resourceName) ? Optional.of(((Number) properties.get(resourceName)).longValue()) : Optional.empty();
  }

  public Optional<String> getResourceAsString(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return properties.containsKey(resourceName) ? Optional.of(properties.get(resourceName).toString()) : Optional.empty();
  }

  public Optional<Object> getResourceAsObject(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return Optional.ofNullable(properties.get(resourceName));
  }

  @JsonAnyGetter
  public Map<String, Object> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MesosResourcesObject that = (MesosResourcesObject) o;
    return java.util.Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(properties);
  }

  @Override
  public String toString() {
    return "MesosResourcesObject{" +
        "properties=" + properties +
        '}';
  }
}
