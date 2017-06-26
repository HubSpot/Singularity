package com.hubspot.mesos.json;

import static com.google.common.base.Preconditions.checkNotNull;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosResourcesObject {

  public abstract ImmutableMap<String, Object> getProperties();

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
    return getProperties().containsKey(resourceName) ? Optional.of(((Number) getProperties().get(resourceName)).intValue()) : Optional.<Integer> absent();
  }

  public Optional<Long> getResourceAsLong(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return getProperties().containsKey(resourceName) ? Optional.of(((Number) getProperties().get(resourceName)).longValue()) : Optional.<Long> absent();
  }

  public Optional<String> getResourceAsString(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return getProperties().containsKey(resourceName) ? Optional.of(getProperties().get(resourceName).toString()) : Optional.<String> absent();
  }

  public Optional<Object> getResourceAsObject(String resourceName) {
    checkNotNull(resourceName, "resourceName is null");
    return Optional.fromNullable(getProperties().get(resourceName));
  }
}
