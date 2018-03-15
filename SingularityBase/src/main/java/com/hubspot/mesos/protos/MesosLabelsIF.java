package com.hubspot.mesos.protos;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosLabelsIF {
  List<MesosParameter> getLabels();
}
