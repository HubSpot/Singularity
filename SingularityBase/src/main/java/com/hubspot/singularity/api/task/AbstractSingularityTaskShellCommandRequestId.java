package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema
public abstract class AbstractSingularityTaskShellCommandRequestId {

  public abstract SingularityTaskId getTaskId();

  public abstract String getName();

  public abstract long getTimestamp();

  @Derived
  @JsonIgnore
  public String getId() {
    return String.format("%s-%s", getTaskId(), getSubIdForTaskHistory());
  }

  @Derived
  @JsonIgnore
  public String getSubIdForTaskHistory() {
    return String.format("%s-%s", getName().replace("/", ""), getTimestamp());
  }
}
