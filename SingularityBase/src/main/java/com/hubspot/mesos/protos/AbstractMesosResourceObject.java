package com.hubspot.mesos.protos;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosResourceObject {
  @Nullable
  public abstract String getName();

  @JsonIgnore
  public boolean hasName() {
    return getName() != null;
  }

  @Nullable
  public abstract MesosRangesObject getRanges();

  @JsonIgnore
  public boolean hasRanges() {
    return getRanges() != null;
  }

  // Unknown fields
  @JsonAnyGetter
  public abstract Map<String, Object> getAllOtherFields();
}
