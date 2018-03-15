package com.hubspot.mesos.protos;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosParameter {

  @Nullable
  public abstract String getKey();

  @JsonIgnore
  public boolean hasKey() {
    return getKey() != null;
  }

  @Nullable
  public abstract String getValue();

  @JsonIgnore
  public boolean hasValue() {
    return getValue() != null;
  }
}
