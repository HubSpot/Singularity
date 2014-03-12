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
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingularityPendingTask other = (SingularityPendingTask) obj;
    if (taskId == null) {
      if (other.taskId != null)
        return false;
    } else if (!taskId.equals(other.taskId))
      return false;
    return true;
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
