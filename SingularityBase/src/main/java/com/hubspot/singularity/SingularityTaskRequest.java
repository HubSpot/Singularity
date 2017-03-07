package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskRequest implements Comparable<SingularityTaskRequest> {

  private final SingularityRequest request;
  private final SingularityDeploy deploy;
  private final SingularityPendingTask pendingTask;

  @JsonCreator
  public SingularityTaskRequest(@JsonProperty("request") SingularityRequest request, @JsonProperty("deploy") SingularityDeploy deploy, @JsonProperty("pendingTask") SingularityPendingTask pendingTask) {
    this.request = request;
    this.deploy = deploy;
    this.pendingTask = pendingTask;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  public SingularityDeploy getDeploy() {
    return deploy;
  }

  public SingularityPendingTask getPendingTask() {
    return pendingTask;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pendingTask.getPendingTaskId());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null) {
        return false;
    }
    if (getClass() != obj.getClass()) {
        return false;
    }
    SingularityTaskRequest other = (SingularityTaskRequest) obj;
    if (pendingTask == null) {
      if (other.pendingTask != null) {
        return false;
    }
    } else if (!pendingTask.getPendingTaskId().equals(other.pendingTask.getPendingTaskId())) {
        return false;
    }
    return true;
  }

  @Override
  public int compareTo(SingularityTaskRequest o) {
    return this.getPendingTask().getPendingTaskId().compareTo(o.getPendingTask().getPendingTaskId());
  }

  @Override
  public String toString() {
    return "SingularityTaskRequest{" +
        "request=" + request +
        ", deploy=" + deploy +
        ", pendingTask=" + pendingTask +
        '}';
  }
}
