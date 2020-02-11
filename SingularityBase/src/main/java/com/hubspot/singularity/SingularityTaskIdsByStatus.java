package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tasks in all states for a particular request")
public class SingularityTaskIdsByStatus {
  private List<SingularityTaskId> healthy;
  private List<SingularityTaskId> notYetHealthy;
  private List<SingularityPendingTaskId> pending;
  private List<SingularityTaskId> cleaning;
  private List<SingularityTaskId> loadBalanced;
  private List<SingularityTaskId> killed;

  @JsonCreator
  public SingularityTaskIdsByStatus(@JsonProperty("healthy") List<SingularityTaskId> healthy,
                                    @JsonProperty("notYetHealthy") List<SingularityTaskId> notYetHealthy,
                                    @JsonProperty("pending") List<SingularityPendingTaskId> pending,
                                    @JsonProperty("cleaning") List<SingularityTaskId> cleaning,
                                    @JsonProperty("loadBalanced") List<SingularityTaskId> loadBalanced,
                                    @JsonProperty("killed") List<SingularityTaskId> killed) {
    this.healthy = healthy;
    this.notYetHealthy = notYetHealthy;
    this.pending = pending;
    this.cleaning = cleaning;
    this.loadBalanced = loadBalanced != null ? loadBalanced : Collections.emptyList();
    this.killed = killed != null ? killed : Collections.emptyList();
  }

  public SingularityTaskIdsByStatus(List<SingularityTaskId> healthy,
                                    List<SingularityTaskId> notYetHealthy,
                                    List<SingularityPendingTaskId> pending,
                                    List<SingularityTaskId> cleaning) {
    this(healthy, notYetHealthy, pending, cleaning, Collections.emptyList(), Collections.emptyList());
  }

  @Schema(description = "Active tasks whose healthchecks and load balancer updates (when applicable) have finished successfully")
  public List<SingularityTaskId> getHealthy() {
    return healthy;
  }

  @Schema(description = "Active tasks whose healthchecks and load balancer updates (when applicable) have not yet finished successfully")
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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SingularityTaskIdsByStatus) {
      final SingularityTaskIdsByStatus that = (SingularityTaskIdsByStatus) obj;
      return Objects.equals(this.healthy, that.healthy) &&
          Objects.equals(this.notYetHealthy, that.notYetHealthy) &&
          Objects.equals(this.pending, that.pending) &&
          Objects.equals(this.cleaning, that.cleaning);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(healthy, notYetHealthy, pending, cleaning);
  }

  @Override
  public String toString() {
    return "SingularityTaskIdsByStatus{" +
        "healthy=" + healthy +
        ", notYetHealthy=" + notYetHealthy +
        ", pending=" + pending +
        ", cleaning=" + cleaning +
        '}';
  }
}
