package com.hubspot.singularity.helpers;

import com.hubspot.singularity.SingularityTask;
import org.apache.mesos.v1.Protos.TaskInfo;

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
