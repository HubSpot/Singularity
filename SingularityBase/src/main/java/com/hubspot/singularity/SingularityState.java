package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the current state of singularity")
public class SingularityState {

  private final int activeTasks;
  private final int launchingTasks;
  private final int pausedRequests;
  private final int activeRequests;
  private final int cooldownRequests;
  private final int scheduledTasks;
  private final int lateTasks;
  private final int futureTasks;
  private final int cleaningTasks;
  private final int lbCleanupTasks;
  private final int lbCleanupRequests;

  private final long maxTaskLag;

  private final int pendingRequests;
  private final int cleaningRequests;
  private final int finishedRequests;

  private final int activeSlaves;
  private final int deadSlaves;
  private final int decommissioningSlaves;
  private final int unknownSlaves;

  private final int activeRacks;
  private final int deadRacks;
  private final int decommissioningRacks;
  private final int unknownRacks;

  private final long oldestDeploy;
  private final int numDeploys;
  private final long oldestDeployStep;
  private final List<SingularityDeployMarker> activeDeploys;

  private final long generatedAt;

  private final List<SingularityHostState> hostStates;

  private final List<String> overProvisionedRequestIds;
  private final List<String> underProvisionedRequestIds;

  private final int overProvisionedRequests;
  private final int underProvisionedRequests;

  private final Optional<Boolean> authDatastoreHealthy;

  private final Optional<Double> minimumPriorityLevel;

  private final long avgStatusUpdateDelayMs;
  private final long lastHeartbeatAt;

  @JsonCreator
  public SingularityState(@JsonProperty("activeTasks") int activeTasks,
                          @JsonProperty("launchingTasks") int launchingTasks,
                          @JsonProperty("activeRequests") int activeRequests,
                          @JsonProperty("cooldownRequests") int cooldownRequests,
                          @JsonProperty("pausedRequests") int pausedRequests,
                          @JsonProperty("scheduledTasks") int scheduledTasks,
                          @JsonProperty("pendingRequests") int pendingRequests,
                          @JsonProperty("lbCleanupTasks") int lbCleanupTasks,
                          @JsonProperty("lbCleanupRequests") int lbCleanupRequests,
                          @JsonProperty("cleaningRequests") int cleaningRequests,
                          @JsonProperty("activeSlaves") int activeSlaves,
                          @JsonProperty("deadSlaves") int deadSlaves,
                          @JsonProperty("decommissioningSlaves") int decommissioningSlaves,
                          @JsonProperty("activeRacks") int activeRacks,
                          @JsonProperty("deadRacks") int deadRacks,
                          @JsonProperty("decommissioningRacks") int decommissioningRacks,
                          @JsonProperty("cleaningTasks") int cleaningTasks,
                          @JsonProperty("hostStates") List<SingularityHostState> hostStates,
                          @JsonProperty("oldestDeploy") long oldestDeploy,
                          @JsonProperty("numDeploys") int numDeploys,
                          @JsonProperty("oldestDeployStep") long oldestDeployStep,
                          @JsonProperty("activeDeploys") List<SingularityDeployMarker> activeDeploys,
                          @JsonProperty("lateTasks") int lateTasks,
                          @JsonProperty("futureTasks") int futureTasks,
                          @JsonProperty("maxTaskLag") long maxTaskLag,
                          @JsonProperty("generatedAt") long generatedAt,
                          @JsonProperty("overProvisionedRequestIds") List<String> overProvisionedRequestIds,
                          @JsonProperty("underProvisionedRequestIds") List<String> underProvisionedRequestIds,
                          @JsonProperty("overProvisionedRequests") int overProvisionedRequests,
                          @JsonProperty("underProvisionedRequests") int underProvisionedRequests,
                          @JsonProperty("finishedRequests") int finishedRequests,
                          @JsonProperty("unknownRacks") int unknownRacks,
                          @JsonProperty("unknownSlaves") int unknownSlaves,
                          @JsonProperty("authDatastoreHealthy") Optional<Boolean> authDatastoreHealthy,
                          @JsonProperty("minimumPriorityLevel") Optional<Double> minimumPriorityLevel,
                          @JsonProperty("avgStatusUpdateDelayMs") long avgStatusUpdateDelayMs,
                          @JsonProperty("lastHeartbeatAt") long lastHeartbeatAt) {
    this.activeTasks = activeTasks;
    this.launchingTasks = launchingTasks;
    this.activeRequests = activeRequests;
    this.pausedRequests = pausedRequests;
    this.cooldownRequests = cooldownRequests;
    this.scheduledTasks = scheduledTasks;
    this.pendingRequests = pendingRequests;
    this.cleaningRequests = cleaningRequests;
    this.activeRacks = activeRacks;
    this.generatedAt = generatedAt;
    this.activeSlaves = activeSlaves;
    this.deadRacks = deadRacks;
    this.deadSlaves = deadSlaves;
    this.unknownSlaves = unknownSlaves;
    this.unknownRacks = unknownRacks;
    this.decommissioningRacks = decommissioningRacks;
    this.decommissioningSlaves = decommissioningSlaves;
    this.cleaningTasks = cleaningTasks;
    this.hostStates = hostStates;
    this.lateTasks = lateTasks;
    this.finishedRequests = finishedRequests;
    this.futureTasks = futureTasks;
    this.maxTaskLag = maxTaskLag;
    this.oldestDeploy = oldestDeploy;
    this.numDeploys = numDeploys;
    this.oldestDeployStep = oldestDeployStep;
    this.activeDeploys = activeDeploys;
    this.lbCleanupTasks = lbCleanupTasks;
    this.lbCleanupRequests = lbCleanupRequests;
    this.underProvisionedRequests = underProvisionedRequests;
    this.overProvisionedRequests = overProvisionedRequests;
    this.overProvisionedRequestIds = overProvisionedRequestIds;
    this.underProvisionedRequestIds = underProvisionedRequestIds;
    this.authDatastoreHealthy = authDatastoreHealthy;
    this.minimumPriorityLevel = minimumPriorityLevel;
    this.avgStatusUpdateDelayMs = avgStatusUpdateDelayMs;
    this.lastHeartbeatAt = lastHeartbeatAt;
  }

  @Schema(description = "Count of requests in finished state")
  public int getFinishedRequests() {
    return finishedRequests;
  }

  @Schema(description = "Time this state was generated")
  public long getGeneratedAt() {
    return generatedAt;
  }

  @Schema(description = "Timestamp of the oldest running deploy")
  public long getOldestDeploy() {
    return oldestDeploy;
  }

  @Schema(description = "Number of active/in-progress deploys")
  public int getNumDeploys() {
    return numDeploys;
  }

  @Schema(
      title = "Timestamp of the oldest single step for a deploy",
      description = "For incremental deploys this is a measure of the time taken for a single step of the deploy"
  )
  public long getOldestDeployStep() {
    return oldestDeployStep;
  }

  @Schema(description = "List of active deploy identifiers")
  public List<SingularityDeployMarker> getActiveDeploys() {
    return activeDeploys;
  }

  @Schema(description = "Count of requests in paused state")
  public int getPausedRequests() {
    return pausedRequests;
  }

  @Schema(description = "Describes the state of all Singularity scheduler instances")
  public List<SingularityHostState> getHostStates() {
    return hostStates;
  }

  @Schema(description = "The count of cleaning tasks")
  public int getCleaningTasks() {
    return cleaningTasks;
  }

  @Schema(description = "The count of active slaves")
  public int getActiveSlaves() {
    return activeSlaves;
  }

  @Schema(description = "The count of dead slaves (no longer reachable or considered lost by mesos)")
  public int getDeadSlaves() {
    return deadSlaves;
  }

  @Schema(description = "The count of slaves currently decommissioning")
  public int getDecommissioningSlaves() {
    return decommissioningSlaves;
  }

  @Deprecated
  @Schema(description = "The count of slaves currently decommissioning")
  public int getDecomissioningSlaves() {
    return decommissioningSlaves;
  }

  @Schema(description = "The count of active racks")
  public int getActiveRacks() {
    return activeRacks;
  }

  @Schema(description = "The count of racks considered dead")
  public int getDeadRacks() {
    return deadRacks;
  }

  @Deprecated
  @Schema(description = "The count of racks that are currently decommissioning")
  public int getDecomissioningRacks() {
    return decommissioningRacks;
  }

  @Schema(description = "The count of racks that are currently decommissioning")
  public int getDecommissioningRacks() {
    return decommissioningRacks;
  }

  @Schema(description = "The count of active tasks")
  public int getActiveTasks() {
    return activeTasks;
  }

  @Schema(description = "The count of tasks in launching state")
  public int getLaunchingTasks() {
    return launchingTasks;
  }

  @Schema(description = "The count of all requests in all states")
  public int getAllRequests() {
    return activeRequests + cooldownRequests + pausedRequests;
  }

  @Schema(description = "The count of requests in active state")
  public int getActiveRequests() {
    return activeRequests;
  }

  @Schema(description = "The count of requests in cooldown state")
  public int getCooldownRequests() {
    return cooldownRequests;
  }

  @Schema(description = "The count of tasks waiting to be launched")
  public int getScheduledTasks() {
    return scheduledTasks;
  }

  @Schema(
      title = "The count of pending requests",
      description = "A pending request is a trigger for the scheduler to perform an action for a request (create a cleanup, launch a task, etc)"
  )
  public int getPendingRequests() {
    return pendingRequests;
  }

  @Schema(description = "The count of requests with associated cleanups (e.g. due to a bounce)")
  public int getCleaningRequests() {
    return cleaningRequests;
  }

  @Schema(description = "The count of tasks that have not been launched in time")
  public int getLateTasks() {
    return lateTasks;
  }

  @Schema(description = "The count of pending tasks that will be launched at a future time")
  public int getFutureTasks() {
    return futureTasks;
  }

  @Schema(description = "The maximum delay in launching any pending task")
  public long getMaxTaskLag() {
    return maxTaskLag;
  }

  @Schema(description = "The count of tasks with associated load balancer cleanups")
  public int getLbCleanupTasks() {
    return lbCleanupTasks;
  }

  @Schema(description = "The count of requests with associated load balancer cleanups")
  public int getLbCleanupRequests() {
    return lbCleanupRequests;
  }

  @Schema(description = "The count of requests running too many instances")
  public List<String> getOverProvisionedRequestIds() {
    return overProvisionedRequestIds;
  }

  @Schema(description = "The ids of requests running too many instances")
  public List<String> getUnderProvisionedRequestIds() {
    return underProvisionedRequestIds;
  }

  @Schema(description = "The count of requests running too few instances")
  public int getOverProvisionedRequests() {
    return overProvisionedRequests;
  }

  @Schema(description = "The ids of requests running too few instances")
  public int getUnderProvisionedRequests() {
    return underProvisionedRequests;
  }

  @Schema(description = "The count of slaves in an unknown state")
  public int getUnknownSlaves() {
    return unknownSlaves;
  }

  @Schema(description = "The count of racks in an unknown state")
  public int getUnknownRacks() {
    return unknownRacks;
  }

  @Schema(description = "`true` if the auth datastore is reachable (when auth is configured)", nullable = true)
  public Optional<Boolean> getAuthDatastoreHealthy() {
    return authDatastoreHealthy;
  }

  @Schema(description = "The minimum priority level for launching tasks if a priority freeze is active, empty otherwise", nullable = true)
  public Optional<Double> getMinimumPriorityLevel() {
    return minimumPriorityLevel;
  }

  @Schema(description = "The average delay (in millis) for processing status updates from mesos")
  public long getAvgStatusUpdateDelayMs() {
    return avgStatusUpdateDelayMs;
  }

  @Schema(description = "Time in UTC millis at which Singularity received the most recent HEARTBEAT event from the Mesos Master")
  public long getLastHeartbeatAt() {
    return lastHeartbeatAt;
  }

  @Override
  public String toString() {
    return "SingularityState{" +
        "activeTasks=" + activeTasks +
        ", launchingTasks=" + launchingTasks +
        ", pausedRequests=" + pausedRequests +
        ", activeRequests=" + activeRequests +
        ", cooldownRequests=" + cooldownRequests +
        ", scheduledTasks=" + scheduledTasks +
        ", lateTasks=" + lateTasks +
        ", futureTasks=" + futureTasks +
        ", cleaningTasks=" + cleaningTasks +
        ", lbCleanupTasks=" + lbCleanupTasks +
        ", lbCleanupRequests=" + lbCleanupRequests +
        ", maxTaskLag=" + maxTaskLag +
        ", pendingRequests=" + pendingRequests +
        ", cleaningRequests=" + cleaningRequests +
        ", finishedRequests=" + finishedRequests +
        ", activeSlaves=" + activeSlaves +
        ", deadSlaves=" + deadSlaves +
        ", decommissioningSlaves=" + decommissioningSlaves +
        ", unknownSlaves=" + unknownSlaves +
        ", activeRacks=" + activeRacks +
        ", deadRacks=" + deadRacks +
        ", decommissioningRacks=" + decommissioningRacks +
        ", unknownRacks=" + unknownRacks +
        ", oldestDeploy=" + oldestDeploy +
        ", numDeploys=" + numDeploys +
        ", oldestDeployStep=" + oldestDeployStep +
        ", activeDeploys=" + activeDeploys +
        ", generatedAt=" + generatedAt +
        ", hostStates=" + hostStates +
        ", overProvisionedRequestIds=" + overProvisionedRequestIds +
        ", underProvisionedRequestIds=" + underProvisionedRequestIds +
        ", overProvisionedRequests=" + overProvisionedRequests +
        ", underProvisionedRequests=" + underProvisionedRequests +
        ", authDatastoreHealthy=" + authDatastoreHealthy +
        ", minimumPriorityLevel=" + minimumPriorityLevel +
        ", avgStatusUpdateDelayMs=" + avgStatusUpdateDelayMs +
        '}';
  }
}
