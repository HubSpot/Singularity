package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Schema(description = "Describes the progress a deploy has made")
public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int currentActiveInstances;
  private final boolean stepLaunchComplete;
  private final Map<String, Boolean> stepAcceptanceResults;
  private final Set<String> acceptanceResultMessageHistory;
  private final Set<SingularityTaskId> failedDeployTasks;
  private final long timestamp;
  private final Map<String, DeployProgressLbUpdateHolder> lbUpdates;
  private final Optional<SingularityLoadBalancerUpdate> pendingLbUpdate;

  public static SingularityDeployProgress forNonLongRunning() {
    return new SingularityDeployProgress(
      1,
      1,
      true,
      Collections.emptySet(),
      System.currentTimeMillis(),
      Collections.emptyMap(),
      Optional.empty(),
      Collections.emptyMap(),
      Collections.emptySet()
    );
  }

  @JsonCreator
  public SingularityDeployProgress(
    @JsonProperty("targetActiveInstances") int targetActiveInstances,
    @JsonProperty("currentActiveInstances") int currentActiveInstances,
    @JsonProperty("stepLaunchComplete") boolean stepLaunchComplete,
    @JsonProperty("failedDeployTasks") Set<SingularityTaskId> failedDeployTasks,
    @JsonProperty("timestamp") long timestamp,
    @JsonProperty("lbUpdates") Map<String, DeployProgressLbUpdateHolder> lbUpdates,
    @JsonProperty(
      "pendingLbUpdate"
    ) Optional<SingularityLoadBalancerUpdate> pendingLbUpdate,
    @JsonProperty("stepAcceptanceResults") Map<String, Boolean> stepAcceptanceResults,
    @JsonProperty(
      "acceptanceResultMessageHistory"
    ) Set<String> acceptanceResultMessageHistory
  ) {
    this.targetActiveInstances = targetActiveInstances;
    this.currentActiveInstances = currentActiveInstances;
    this.stepLaunchComplete = stepLaunchComplete;
    this.failedDeployTasks = failedDeployTasks;
    this.timestamp = timestamp;
    this.lbUpdates = lbUpdates == null ? new HashMap<>() : lbUpdates;
    this.pendingLbUpdate = pendingLbUpdate;
    this.stepAcceptanceResults =
      stepAcceptanceResults == null ? new HashMap<>() : stepAcceptanceResults;
    this.acceptanceResultMessageHistory =
      acceptanceResultMessageHistory == null
        ? new HashSet<>()
        : acceptanceResultMessageHistory;
  }

  @Schema(description = "The desired number of instances for the current deploy step")
  public int getTargetActiveInstances() {
    return targetActiveInstances;
  }

  @Schema(description = "The current number of active tasks for this deploy")
  public int getCurrentActiveInstances() {
    return currentActiveInstances;
  }

  @Schema(
    description = "`true` if the current deploy step has completed launch instances (and adding to load balancer)"
  )
  public boolean isStepLaunchComplete() {
    return stepLaunchComplete;
  }

  @Schema(description = "Results from configured post-deploy step checks")
  public Map<String, Boolean> getStepAcceptanceResults() {
    return stepAcceptanceResults;
  }

  @Schema(description = "Messages from all previously run deploy checks")
  public Set<String> getAcceptanceResultMessageHistory() {
    return acceptanceResultMessageHistory;
  }

  @Schema(description = "Tasks for this deploy that have failed so far")
  public Set<SingularityTaskId> getFailedDeployTasks() {
    return failedDeployTasks;
  }

  @Schema(description = "The timestamp of this deploy progress update")
  public long getTimestamp() {
    return timestamp;
  }

  @Schema(description = "Load balancer updates relevant for a deploy")
  public Map<String, DeployProgressLbUpdateHolder> getLbUpdates() {
    return lbUpdates;
  }

  @Schema(description = "load balancer updates that are in progress")
  public Optional<SingularityLoadBalancerUpdate> getPendingLbUpdate() {
    return pendingLbUpdate;
  }

  public SingularityDeployProgress withNewTargetInstances(int instances) {
    return new SingularityDeployProgress(
      instances,
      currentActiveInstances,
      false,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdates,
      pendingLbUpdate,
      stepAcceptanceResults,
      acceptanceResultMessageHistory
    );
  }

  public SingularityDeployProgress withNewActiveInstances(int instances) {
    return new SingularityDeployProgress(
      targetActiveInstances,
      instances,
      false,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdates,
      pendingLbUpdate,
      stepAcceptanceResults,
      acceptanceResultMessageHistory
    );
  }

  public SingularityDeployProgress withCompletedStepLaunch() {
    return new SingularityDeployProgress(
      targetActiveInstances,
      currentActiveInstances,
      true,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdates,
      pendingLbUpdate,
      stepAcceptanceResults,
      acceptanceResultMessageHistory
    );
  }

  public SingularityDeployProgress withFailedTasks(Set<SingularityTaskId> failedTasks) {
    return new SingularityDeployProgress(
      targetActiveInstances,
      currentActiveInstances,
      false,
      failedTasks,
      System.currentTimeMillis(),
      lbUpdates,
      pendingLbUpdate,
      stepAcceptanceResults,
      acceptanceResultMessageHistory
    );
  }

  public SingularityDeployProgress withPendingLbUpdate(
    SingularityLoadBalancerUpdate loadBalancerUpdate
  ) {
    return withPendingLbUpdate(
      loadBalancerUpdate,
      Collections.emptySet(),
      Collections.emptySet()
    );
  }

  public SingularityDeployProgress withPendingLbUpdate(
    SingularityLoadBalancerUpdate loadBalancerUpdate,
    Collection<SingularityTaskId> added,
    Collection<SingularityTaskId> removed
  ) {
    Map<String, DeployProgressLbUpdateHolder> lbUpdateMap = new HashMap<>(lbUpdates);
    lbUpdateMap.put(
      loadBalancerUpdate.getLoadBalancerRequestId().getId(),
      new DeployProgressLbUpdateHolder(
        loadBalancerUpdate,
        new HashSet<>(added),
        new HashSet<>(removed)
      )
    );
    return new SingularityDeployProgress(
      targetActiveInstances,
      currentActiveInstances,
      false,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdateMap,
      Optional.of(loadBalancerUpdate),
      stepAcceptanceResults,
      acceptanceResultMessageHistory
    );
  }

  public SingularityDeployProgress withFinishedLbUpdate(
    SingularityLoadBalancerUpdate loadBalancerUpdate,
    DeployProgressLbUpdateHolder lbUpdateHolder
  ) {
    Map<String, DeployProgressLbUpdateHolder> lbUpdateMap = new HashMap<>(lbUpdates);
    lbUpdateMap.put(
      loadBalancerUpdate.getLoadBalancerRequestId().getId(),
      new DeployProgressLbUpdateHolder(
        loadBalancerUpdate,
        lbUpdateHolder.getAdded(),
        lbUpdateHolder.getRemoved()
      )
    );
    return new SingularityDeployProgress(
      targetActiveInstances,
      currentActiveInstances,
      false,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdateMap,
      Optional.empty(),
      stepAcceptanceResults,
      acceptanceResultMessageHistory
    );
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("targetActiveInstances", targetActiveInstances)
      .add("currentActiveInstances", currentActiveInstances)
      .add("stepLaunchComplete", stepLaunchComplete)
      .add("stepAcceptanceResults", stepAcceptanceResults)
      .add("acceptanceResultMessageHistory", acceptanceResultMessageHistory)
      .add("failedDeployTasks", failedDeployTasks)
      .add("timestamp", timestamp)
      .add("lbUpdates", lbUpdates)
      .add("pendingLbUpdate", pendingLbUpdate)
      .toString();
  }
}
