package com.hubspot.mesos.protos;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosRangesObject {

  @Default
  public List<MesosRangeObject> getRange() {
    return Collections.emptyList();
  }

  @JsonIgnore // to mimic mesos
  public List<MesosRangeObject> getRangesList() {
    return getRange();
  }

  @JsonIgnore
  public boolean hasRange() {
    return !getRange().isEmpty();
  }
}
