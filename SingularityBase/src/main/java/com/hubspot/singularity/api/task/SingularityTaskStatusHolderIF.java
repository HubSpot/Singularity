package com.hubspot.singularity.api.task;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.mesos.protos.MesosTaskStatusObject;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface SingularityTaskStatusHolderIF {
  SingularityTaskId getTaskId();

  Optional<MesosTaskStatusObject> getTaskStatus();

  long getServerTimestamp();

  String getServerId();

  Optional<String> getSlaveId();
}
