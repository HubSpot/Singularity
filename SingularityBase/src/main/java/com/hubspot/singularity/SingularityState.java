package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

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

  @JsonCreator
  public SingularityState(@JsonProperty("activeTasks") int activeTasks, @JsonProperty("launchingTasks") int launchingTasks, @JsonProperty("activeRequests") int activeRequests, @JsonProperty("cooldownRequests") int cooldownRequests,
                          @JsonProperty("pausedRequests") int pausedRequests, @JsonProperty("scheduledTasks") int scheduledTasks, @JsonProperty("pendingRequests") int pendingRequests, @JsonProperty("lbCleanupTasks") int lbCleanupTasks,
                          @JsonProperty("lbCleanupRequests") int lbCleanupRequests, @JsonProperty("cleaningRequests") int cleaningRequests, @JsonProperty("activeSlaves") int activeSlaves, @JsonProperty("deadSlaves") int deadSlaves,
                          @JsonProperty("decommissioningSlaves") int decommissioningSlaves, @JsonProperty("activeRacks") int activeRacks, @JsonProperty("deadRacks") int deadRacks, @JsonProperty("decommissioningRacks") int decommissioningRacks,
                          @JsonProperty("cleaningTasks") int cleaningTasks, @JsonProperty("hostStates") List<SingularityHostState> hostStates, @JsonProperty("oldestDeploy") long oldestDeploy, @JsonProperty("numDeploys") int numDeploys,
                          @JsonProperty("oldestDeployStep") long oldestDeployStep, @JsonProperty("activeDeploys") List<SingularityDeployMarker> activeDeploys,
                          @JsonProperty("lateTasks") int lateTasks, @JsonProperty("futureTasks") int futureTasks, @JsonProperty("maxTaskLag") long maxTaskLag, @JsonProperty("generatedAt") long generatedAt,
                          @JsonProperty("overProvisionedRequestIds") List<String> overProvisionedRequestIds, @JsonProperty("underProvisionedRequestIds") List<String> underProvisionedRequestIds,
                          @JsonProperty("overProvisionedRequests") int overProvisionedRequests, @JsonProperty("underProvisionedRequests") int underProvisionedRequests, @JsonProperty("finishedRequests") int finishedRequests,
                          @JsonProperty("unknownRacks") int unknownRacks, @JsonProperty("unknownSlaves") int unknownSlaves, @JsonProperty("authDatastoreHealthy") Optional<Boolean> authDatastoreHealthy, @JsonProperty("minimumPriorityLevel") Optional<Double> minimumPriorityLevel,
                          @JsonProperty("avgStatusUpdateDelayMs") long avgStatusUpdateDelayMs) {
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
  }

  public int getFinishedRequests() {
    return finishedRequests;
  }

  public long getGeneratedAt() {
    return generatedAt;
  }

  public long getOldestDeploy() {
    return oldestDeploy;
  }

  public int getNumDeploys() {
    return numDeploys;
  }

  public long getOldestDeployStep() {
    return oldestDeployStep;
  }

  public List<SingularityDeployMarker> getActiveDeploys() {
    return activeDeploys;
  }

  public int getPausedRequests() {
    return pausedRequests;
  }

  public List<SingularityHostState> getHostStates() {
    return hostStates;
  }

  public int getCleaningTasks() {
    return cleaningTasks;
  }

  public int getActiveSlaves() {
    return activeSlaves;
  }

  public int getDeadSlaves() {
    return deadSlaves;
  }

  public int getDecommissioningSlaves() {
    return decommissioningSlaves;
  }

  @Deprecated
  public int getDecomissioningSlaves() {
    return decommissioningSlaves;
  }

  public int getActiveRacks() {
    return activeRacks;
  }

  public int getDeadRacks() {
    return deadRacks;
  }

  @Deprecated
  public int getDecomissioningRacks() {
    return decommissioningRacks;
  }

  public int getDecommissioningRacks() {
    return decommissioningRacks;
  }

  public int getActiveTasks() {
    return activeTasks;
  }

  public int getLaunchingTasks() {
    return launchingTasks;
  }

  public int getAllRequests() {
    return getActiveRequests() + getCooldownRequests() + getPausedRequests();
  }

  public int getActiveRequests() {
    return activeRequests;
  }

  public int getCooldownRequests() {
    return cooldownRequests;
  }

  public int getScheduledTasks() {
    return scheduledTasks;
  }

  public int getPendingRequests() {
    return pendingRequests;
  }

  public int getCleaningRequests() {
    return cleaningRequests;
  }

  public int getLateTasks() {
    return lateTasks;
  }

  public int getFutureTasks() {
    return futureTasks;
  }

  public long getMaxTaskLag() {
    return maxTaskLag;
  }

  public int getLbCleanupTasks() {
    return lbCleanupTasks;
  }

  public int getLbCleanupRequests() {
    return lbCleanupRequests;
  }

  public List<String> getOverProvisionedRequestIds() {
    return overProvisionedRequestIds;
  }

  public List<String> getUnderProvisionedRequestIds() {
    return underProvisionedRequestIds;
  }

  public int getOverProvisionedRequests() {
    return overProvisionedRequests;
  }

  public int getUnderProvisionedRequests() {
    return underProvisionedRequests;
  }

  public int getUnknownSlaves() {
    return unknownSlaves;
  }

  public int getUnknownRacks() {
    return unknownRacks;
  }

  public Optional<Boolean> getAuthDatastoreHealthy() {
    return authDatastoreHealthy;
  }

  public Optional<Double> getMinimumPriorityLevel() {
    return minimumPriorityLevel;
  }

  public long getAvgStatusUpdateDelayMs() {
    return avgStatusUpdateDelayMs;
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
