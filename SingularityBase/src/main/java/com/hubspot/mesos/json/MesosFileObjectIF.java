package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosFileObjectIF {
  String getGid();

  String getMode();

  long getMtime();

  int getNlink();

  String getPath();

  long getSize();

  String getUid();
}
