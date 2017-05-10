package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = MesosFileObject.class)
public interface MesosFileObjectIF {
  String getGid();

  String getMode();

  long getMtime();

  int getNlink();

  String getPath();

  long getSize();

  String getUid();
}
