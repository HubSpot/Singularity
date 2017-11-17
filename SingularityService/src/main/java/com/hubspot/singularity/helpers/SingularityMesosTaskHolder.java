package com.hubspot.singularity.helpers;

import org.apache.mesos.v1.Protos.TaskInfo;

import com.hubspot.singularity.SingularityTask;

public class SingularityMesosTaskHolder {
  private final SingularityTask task;
  private final TaskInfo mesosTask;

  public SingularityMesosTaskHolder(SingularityTask task, TaskInfo mesosTask) {
    this.task = task;
    this.mesosTask = mesosTask;
  }

  public SingularityTask getTask() {
    return task;
  }

  public TaskInfo getMesosTask() {
    return mesosTask;
  }
}
