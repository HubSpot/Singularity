package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = MesosFileChunkObject.class)
public interface MesosFileChunkObjectIF {
  String getData();

  long getOffset();

  Optional<Long> getNextOffset();
}
