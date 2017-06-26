package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosFileChunkObjectIF {
  String getData();

  long getOffset();

  Optional<Long> getNextOffset();
}
