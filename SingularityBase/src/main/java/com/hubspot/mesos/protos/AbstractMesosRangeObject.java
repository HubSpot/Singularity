package com.hubspot.mesos.protos;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosRangeObject {
  @Nullable
  public abstract Long getBegin();

  @JsonIgnore
  public boolean hasBegin() {
    return getBegin() != null;
  }

  @Nullable
  public abstract Long getEnd();

  @JsonIgnore
  public boolean hasEnd() {
    return getEnd() != null;
  }
}
