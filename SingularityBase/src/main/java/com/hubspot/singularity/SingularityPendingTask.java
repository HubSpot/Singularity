package com.hubspot.singularity;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class SingularityPendingTask {

  private final SingularityPendingTaskId taskId;
  private final Optional<String> maybeCmdLineArgs;
  
  public SingularityPendingTask(SingularityPendingTaskId taskId, Optional<String> maybeCmdLineArgs) {
    this.taskId = taskId;
    this.maybeCmdLineArgs = maybeCmdLineArgs;
  }
  
  public static List<SingularityPendingTask> filter(List<SingularityPendingTask> tasks, String requestId) {
    List<SingularityPendingTask> matching = Lists.newArrayList();
    for (SingularityPendingTask task : tasks) {
      if (task.getTaskId().getRequestId().equals(requestId)) {
        matching.add(task);
      }
    }
    return matching;
  }

  public SingularityPendingTaskId getTaskId() {
    return taskId;
  }

  public Optional<String> getMaybeCmdLineArgs() {
    return maybeCmdLineArgs;
  }

  @Override
  public String toString() {
    return "SingularityPendingTask [taskId=" + taskId + ", maybeCmdLineArgs=" + maybeCmdLineArgs + "]";
  }
  
}
