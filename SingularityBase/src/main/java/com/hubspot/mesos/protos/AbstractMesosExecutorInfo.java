package com.hubspot.mesos.protos;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosExecutorInfo {
  @Nullable
  public abstract MesosStringValue getExecutorId();

  @JsonIgnore
  public boolean hasExecutorId() {
    return getExecutorId() != null;
  }

  // Unknown fields
  @JsonAnyGetter
  public abstract Map<String, Object> getUnknownFields();
}
