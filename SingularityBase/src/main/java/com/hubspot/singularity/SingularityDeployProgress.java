package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Schema(description = "Describes the progress a deploy has made")
public class SingularityDeployProgress {
  private final int targetActiveInstances;
  private final int currentActiveInstances;
  private final boolean stepLaunchComplete;
  private final Map<String, DeployAcceptanceState> stepAcceptanceResults;
  private final List<String> acceptanceResultMessageHistory;
  private final Set<SingularityTaskId> failedDeployTasks;
  private final long timestamp;
  private final Map<String, DeployProgressLbUpdateHolder> lbUpdates;
  private final Optional<SingularityLoadBalancerUpdate> pendingLbUpdate;
  private final boolean canary;

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
      Collections.emptyList(),
      false
    );
  }

  public static SingularityDeployProgress forNewDeploy(
    int firstTargetInstances,
    boolean canary
  ) {
    return forNewDeploy(firstTargetInstances, System.currentTimeMillis(), canary);
  }

  public static SingularityDeployProgress forNewDeploy(
    int firstTargetInstances,
    long timestamp,
    boolean canary
  ) {
    return new SingularityDeployProgress(
      firstTargetInstances,
      0,
      false,
      Collections.emptySet(),
      timestamp,
      Collections.emptyMap(),
      Optional.empty(),
      Collections.emptyMap(),
      Collections.emptyList(),
      canary
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
    @JsonProperty(
      "stepAcceptanceResults"
    ) Map<String, DeployAcceptanceState> stepAcceptanceResults,
    @JsonProperty(
      "acceptanceResultMessageHistory"
    ) List<String> acceptanceResultMessageHistory,
    @JsonProperty("canary") boolean canary
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
        ? new ArrayList<>()
        : acceptanceResultMessageHistory;
    this.canary = canary;
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
  public Map<String, DeployAcceptanceState> getStepAcceptanceResults() {
    return stepAcceptanceResults;
  }

  @Schema(description = "Messages from all previously run deploy checks")
  public List<String> getAcceptanceResultMessageHistory() {
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

  @Schema(description = "True if this is a canary deploy")
  public boolean isCanary() {
    return canary;
  }

  public SingularityDeployProgress withNewTargetInstances(
    int instances,
    int currentActive,
    boolean resetAcceptanceResults
  ) {
    return new SingularityDeployProgress(
      instances,
      currentActive,
      false,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdates,
      pendingLbUpdate,
      resetAcceptanceResults ? new HashMap<>() : stepAcceptanceResults,
      acceptanceResultMessageHistory,
      canary
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
      acceptanceResultMessageHistory,
      canary
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
      acceptanceResultMessageHistory,
      canary
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
      acceptanceResultMessageHistory,
      canary
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
      loadBalancerUpdate.getLoadBalancerRequestId().toString(),
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
      acceptanceResultMessageHistory,
      canary
    );
  }

  public SingularityDeployProgress withFinishedLbUpdate(
    SingularityLoadBalancerUpdate loadBalancerUpdate,
    DeployProgressLbUpdateHolder lbUpdateHolder
  ) {
    Map<String, DeployProgressLbUpdateHolder> lbUpdateMap = new HashMap<>(lbUpdates);
    lbUpdateMap.put(
      loadBalancerUpdate.getLoadBalancerRequestId().toString(),
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
      acceptanceResultMessageHistory,
      canary
    );
  }

  public SingularityDeployProgress withAcceptanceProgress(
    Map<String, DeployAcceptanceResult> results
  ) {
    Map<String, DeployAcceptanceState> updatedResults = new HashMap<>(
      stepAcceptanceResults
    );
    List<String> resultHistory = new ArrayList<>(acceptanceResultMessageHistory);
    results.forEach(
      (name, result) -> {
        resultHistory.add(
          String.format("(%s - %s) %s", name, result.getState(), result.getMessage())
        );
        updatedResults.put(name, result.getState());
      }
    );
    return new SingularityDeployProgress(
      targetActiveInstances,
      currentActiveInstances,
      stepLaunchComplete,
      failedDeployTasks,
      System.currentTimeMillis(),
      lbUpdates,
      pendingLbUpdate,
      updatedResults,
      resultHistory,
      canary
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
      .add("canary", canary)
      .toString();
  }
}
