package com.hubspot.mesos.protos;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosStringValueIF {
  String getValue();
}
