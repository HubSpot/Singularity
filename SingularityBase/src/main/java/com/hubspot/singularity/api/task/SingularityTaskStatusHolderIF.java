package com.hubspot.singularity.api.task;

import java.util.Optional;

import com.hubspot.mesos.protos.MesosTaskStatusObject;

public interface SingularityTaskStatusHolderIF {
  Optional<MesosTaskStatusObject> getTaskStatus();

  SingularityTaskId getTaskId();

  long getServerTimestamp();

  String getServerId();

  Optional<String> getSlaveId();
}
