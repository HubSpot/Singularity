package com.hubspot.mesos.protos;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractMesosRangesObject {

  public abstract List<MesosRangeObject> getRange();

  @JsonIgnore // to mimic mesos
  public List<MesosRangeObject> getRangesList() {
    return getRange();
  }

  @JsonIgnore
  public boolean hasRange() {
    return !getRange().isEmpty();
  }
}
