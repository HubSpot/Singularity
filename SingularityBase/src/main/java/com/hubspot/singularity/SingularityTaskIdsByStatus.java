package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Schema(description = "Tasks in all states for a particular request")
public class SingularityTaskIdsByStatus {
  private List<SingularityTaskId> healthy;
  private List<SingularityTaskId> notYetHealthy;
  private List<SingularityPendingTaskId> pending;
  private List<SingularityTaskId> cleaning;
  private List<SingularityTaskId> loadBalanced;
  private List<SingularityTaskId> killed;

  @JsonCreator
  public SingularityTaskIdsByStatus(
    @JsonProperty("healthy") List<SingularityTaskId> healthy,
    @JsonProperty("notYetHealthy") List<SingularityTaskId> notYetHealthy,
    @JsonProperty("pending") List<SingularityPendingTaskId> pending,
    @JsonProperty("cleaning") List<SingularityTaskId> cleaning,
    @JsonProperty("loadBalanced") List<SingularityTaskId> loadBalanced,
    @JsonProperty("killed") List<SingularityTaskId> killed
  ) {
    this.healthy = healthy;
    this.notYetHealthy = notYetHealthy;
    this.pending = pending;
    this.cleaning = cleaning;
    this.loadBalanced = loadBalanced != null ? loadBalanced : Collections.emptyList();
    this.killed = killed != null ? killed : Collections.emptyList();
  }

  @Schema(
    description = "Active tasks whose healthchecks and load balancer updates (when applicable) have finished successfully"
  )
  public List<SingularityTaskId> getHealthy() {
    return healthy;
  }

  @Schema(
    description = "Active tasks whose healthchecks and load balancer updates (when applicable) have not yet finished successfully"
  )
  public List<SingularityTaskId> getNotYetHealthy() {
    return notYetHealthy;
  }

  @Schema(description = "Tasks that have not yet been launched")
  public List<SingularityPendingTaskId> getPending() {
    return pending;
  }

  @Schema(description = "Active tasks in a cleaning state")
  public List<SingularityTaskId> getCleaning() {
    return cleaning;
  }

  @Schema(description = "Tasks that are currently active in the load balancer")
  public List<SingularityTaskId> getLoadBalanced() {
    return loadBalanced;
  }

  @Schema(description = "Tasks which have been sent a kill signal")
  public List<SingularityTaskId> getKilled() {
    return killed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityTaskIdsByStatus that = (SingularityTaskIdsByStatus) o;

    if (healthy != null ? !healthy.equals(that.healthy) : that.healthy != null) {
      return false;
    }
    if (
      notYetHealthy != null
        ? !notYetHealthy.equals(that.notYetHealthy)
        : that.notYetHealthy != null
    ) {
      return false;
    }
    if (pending != null ? !pending.equals(that.pending) : that.pending != null) {
      return false;
    }
    if (cleaning != null ? !cleaning.equals(that.cleaning) : that.cleaning != null) {
      return false;
    }
    if (
      loadBalanced != null
        ? !loadBalanced.equals(that.loadBalanced)
        : that.loadBalanced != null
    ) {
      return false;
    }
    return killed != null ? killed.equals(that.killed) : that.killed == null;
  }

  @Override
  public int hashCode() {
    int result = healthy != null ? healthy.hashCode() : 0;
    result = 31 * result + (notYetHealthy != null ? notYetHealthy.hashCode() : 0);
    result = 31 * result + (pending != null ? pending.hashCode() : 0);
    result = 31 * result + (cleaning != null ? cleaning.hashCode() : 0);
    result = 31 * result + (loadBalanced != null ? loadBalanced.hashCode() : 0);
    result = 31 * result + (killed != null ? killed.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return (
      "SingularityTaskIdsByStatus{" +
      "healthy=" +
      healthy +
      ", notYetHealthy=" +
      notYetHealthy +
      ", pending=" +
      pending +
      ", cleaning=" +
      cleaning +
      ", loadBalanced=" +
      loadBalanced +
      ", killed=" +
      killed +
      '}'
    );
  }
}
