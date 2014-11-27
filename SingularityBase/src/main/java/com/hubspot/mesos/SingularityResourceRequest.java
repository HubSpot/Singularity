package com.hubspot.mesos;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * Represents a resource request.
 */
public class SingularityResourceRequest {
  public static final String CPU_RESOURCE_NAME = "cpus";
  public static final String MEMORY_RESOURCE_NAME = "memoryMb";
  public static final String PORT_COUNT_RESOURCE_NAME = "numPorts";

  public static final ImmutableList<String> STANDARD_RESOURCE_NAMES = ImmutableList.of(CPU_RESOURCE_NAME, MEMORY_RESOURCE_NAME, PORT_COUNT_RESOURCE_NAME);

  private final String name;
  private final Object value;

  @JsonCreator
  public SingularityResourceRequest(@JsonProperty("name") String name,
      @JsonProperty("value") Object value) {
    this.name = checkNotNull(name, "name is null");
    this.value = checkNotNull(value, "value is null");
  }

  @JsonProperty
  public String getName() {
    return name;
  }

  @JsonProperty
  public Object getValue() {
    return value;
  }

  @JsonIgnore
  public Number getValueAsNumberOrDefault(Number defaultValue) {
    return value instanceof Number ? (Number) value : defaultValue;
  }

  @JsonIgnore
  public String getValueAsString() {
    return value.toString();
  }

  @JsonIgnore
  public Set<? extends Number> getValueAsNumberSet() {
    ImmutableSet.Builder<Number> builder = ImmutableSet.builder();

    if (value instanceof Number) {
      builder.add((Number) value);
    } else if (value instanceof List<?>) {
      for (Object listValue : (List<?>) value) {
        if (listValue instanceof Number) {
          builder.add((Number) listValue);
        }
      }
    }
    return builder.build();
  }

  @JsonIgnore
  public Set<String> getValueAsStringSet() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();

    if (value instanceof List<?>) {
      for (Object listValue : (List<?>) value) {
        builder.add(listValue.toString());
      }
    } else {
      builder.add(value.toString());
    }

    return builder.build();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static final boolean hasResource(Iterable<SingularityResourceRequest> resources, String name) {
    checkNotNull(resources, "resources is null");
    checkNotNull(name, "name is null");

    for (SingularityResourceRequest resource : resources) {
      if (name.equals(resource.getName())) {
        return true;
      }
    }

    return false;
  }

  public static final Number findNumberResourceRequest(Iterable<SingularityResourceRequest> resources, String name, Number defaultValue) {
    checkNotNull(resources, "resources is null");
    checkNotNull(name, "name is null");
    checkNotNull(defaultValue, "defaultValue is null");

    for (SingularityResourceRequest resource : resources) {
      if (name.equals(resource.getName())) {
        return resource.getValueAsNumberOrDefault(defaultValue);
      }
    }

    return defaultValue;
  }

  public static final String findStringResourceRequest(Iterable<SingularityResourceRequest> resources, String name, @Nullable String defaultValue) {
    checkNotNull(resources, "resources is null");
    checkNotNull(name, "name is null");

    for (SingularityResourceRequest resource : resources) {
      if (name.equals(resource.getName())) {
        return resource.getValueAsString();
      }
    }

    return defaultValue;
  }

  public static Predicate<? super SingularityResourceRequest> getFilterStandardResourcesFunction() {
    return new Predicate<SingularityResourceRequest>() {

      @Override
      public boolean apply(@Nonnull SingularityResourceRequest resourceRequest) {
        return !STANDARD_RESOURCE_NAMES.contains(resourceRequest.getName());
      }
    };
  }

  public static Function<SingularityResourceRequest, String> getNameFunction() {
    return new Function<SingularityResourceRequest, String>() {
      @Override
      public String apply(@Nonnull SingularityResourceRequest request) {
        return request.getName();
      }
    };
  }

  public static Map<String, SingularityResourceRequest> getResourcesAsMap(Iterable<SingularityResourceRequest> resources) {
    checkNotNull(resources, "resources is null");
    return ImmutableMap.copyOf(Maps.uniqueIndex(resources, getNameFunction()));
  }
}
