package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

public class SingularityPendingTask extends SingularityJsonObject {

  private final SingularityPendingTaskId taskId;
  private final Optional<String> maybeCmdLineArgs;
  
  public static Predicate<SingularityPendingTask> matchingRequest(final String requestId) {
    return new Predicate<SingularityPendingTask>() {

      @Override
      public boolean apply(SingularityPendingTask input) {
        return input.getPendingTaskId().getRequestId().equals(requestId);
      }
      
    };
  }
  
  public static Predicate<SingularityPendingTask> matchingDeploy(final String deployId) {
    return new Predicate<SingularityPendingTask>() {

      @Override
      public boolean apply(SingularityPendingTask input) {
        return input.getPendingTaskId().getDeployId().equals(deployId);
      }
      
    };
  }
  
  @JsonCreator
  public SingularityPendingTask(@JsonProperty("pendingTaskId") SingularityPendingTaskId taskId, @JsonProperty("cmdLineArgs") Optional<String> maybeCmdLineArgs) {
    this.taskId = taskId;
    this.maybeCmdLineArgs = maybeCmdLineArgs;
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

  public SingularityPendingTaskId getPendingTaskId() {
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
