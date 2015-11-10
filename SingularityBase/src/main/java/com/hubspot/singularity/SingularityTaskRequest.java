package com.hubspot.singularity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityTaskRequest implements Comparable<SingularityTaskRequest> {

  private final SingularityRequest request;
  private final SingularityDeploy deploy;
  private final SingularityPendingTask pendingTask;
  private Map<String, String> offerDeclinedReasons = new HashMap<>();

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

  public Map<String, String> getOfferDeclinedReasons() {
    return offerDeclinedReasons;
  }

  public void addOfferDeclinedReason(String host, String reason) {
    offerDeclinedReasons.put(host, reason);
  }

  public void clearOfferDeclinedReasons() {
    offerDeclinedReasons.clear();
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
    return "SingularityTaskRequest [request=" + request + ", deploy=" + deploy + ", pendingTask=" + pendingTask + "]";
  }

}
