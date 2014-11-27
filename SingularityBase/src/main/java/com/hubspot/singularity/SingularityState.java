package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityState {

  private final int activeTasks;
  private final int pausedRequests;
  private final int activeRequests;
  private final int cooldownRequests;
  private final int scheduledTasks;
  private final int lateTasks;
  private final int futureTasks;
  private final int cleaningTasks;
  private final int lbCleanupTasks;

  private final long maxTaskLag;

  private final int pendingRequests;
  private final int cleaningRequests;
  private final int finishedRequests;

  private final int activeSlaves;
  private final int deadSlaves;
  private final int decomissioningSlaves;

  private final int activeRacks;
  private final int deadRacks;
  private final int decomissioningRacks;

  private final long oldestDeploy;
  private final int numDeploys;

  private final long generatedAt;

  private final List<SingularityHostState> hostStates;

  private final List<String> overProvisionedRequestIds;
  private final List<String> underProvisionedRequestIds;

  private final int overProvisionedRequests;
  private final int underProvisionedRequests;

  @JsonCreator
  public SingularityState(@JsonProperty("activeTasks") int activeTasks, @JsonProperty("activeRequests") int activeRequests, @JsonProperty("cooldownRequests") int cooldownRequests,
      @JsonProperty("pausedRequests") int pausedRequests, @JsonProperty("scheduledTasks") int scheduledTasks, @JsonProperty("pendingRequests") int pendingRequests, @JsonProperty("lbCleanupTasks") int lbCleanupTasks,
      @JsonProperty("cleaningRequests") int cleaningRequests, @JsonProperty("activeSlaves") int activeSlaves, @JsonProperty("deadSlaves") int deadSlaves,
      @JsonProperty("decomissioningSlaves") int decomissioningSlaves, @JsonProperty("activeRacks") int activeRacks, @JsonProperty("deadRacks") int deadRacks, @JsonProperty("decomissioningRacks") int decomissioningRacks,
      @JsonProperty("cleaningTasks") int cleaningTasks, @JsonProperty("hostStates") List<SingularityHostState> hostStates, @JsonProperty("oldestDeploy") long oldestDeploy, @JsonProperty("numDeploys") int numDeploys,
      @JsonProperty("lateTasks") int lateTasks, @JsonProperty("futureTasks") int futureTasks, @JsonProperty("maxTaskLag") long maxTaskLag, @JsonProperty("generatedAt") long generatedAt,
      @JsonProperty("overProvisionedRequestIds") List<String> overProvisionedRequestIds, @JsonProperty("underProvisionedRequestIds") List<String> underProvisionedRequestIds,
      @JsonProperty("overProvisionedRequests") int overProvisionedRequests, @JsonProperty("underProvisionedRequests") int underProvisionedRequests, @JsonProperty("finishedRequests") int finishedRequests
      ) {
    this.activeTasks = activeTasks;
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
    this.decomissioningRacks = decomissioningRacks;
    this.decomissioningSlaves = decomissioningSlaves;
    this.cleaningTasks = cleaningTasks;
    this.hostStates = hostStates;
    this.lateTasks = lateTasks;
    this.finishedRequests = finishedRequests;
    this.futureTasks = futureTasks;
    this.maxTaskLag = maxTaskLag;
    this.oldestDeploy = oldestDeploy;
    this.numDeploys = numDeploys;
    this.lbCleanupTasks = lbCleanupTasks;
    this.underProvisionedRequests = underProvisionedRequests;
    this.overProvisionedRequests = overProvisionedRequests;
    this.overProvisionedRequestIds = overProvisionedRequestIds;
    this.underProvisionedRequestIds = underProvisionedRequestIds;
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

  public int getDecomissioningSlaves() {
    return decomissioningSlaves;
  }

  public int getActiveRacks() {
    return activeRacks;
  }

  public int getDeadRacks() {
    return deadRacks;
  }

  public int getDecomissioningRacks() {
    return decomissioningRacks;
  }

  public int getActiveTasks() {
    return activeTasks;
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

  @Override
  public String toString() {
    return "SingularityState [activeTasks=" + activeTasks + ", pausedRequests=" + pausedRequests + ", activeRequests=" + activeRequests + ", cooldownRequests=" + cooldownRequests + ", scheduledTasks=" + scheduledTasks + ", lateTasks="
        + lateTasks + ", futureTasks=" + futureTasks + ", cleaningTasks=" + cleaningTasks + ", lbCleanupTasks=" + lbCleanupTasks + ", maxTaskLag=" + maxTaskLag + ", pendingRequests=" + pendingRequests + ", cleaningRequests="
        + cleaningRequests + ", finishedRequests=" + finishedRequests + ", activeSlaves=" + activeSlaves + ", deadSlaves=" + deadSlaves + ", decomissioningSlaves=" + decomissioningSlaves + ", activeRacks=" + activeRacks + ", deadRacks="
        + deadRacks + ", decomissioningRacks=" + decomissioningRacks + ", oldestDeploy=" + oldestDeploy + ", numDeploys=" + numDeploys + ", generatedAt=" + generatedAt + ", hostStates=" + hostStates + ", overProvisionedRequestIds="
        + overProvisionedRequestIds + ", underProvisionedRequestIds=" + underProvisionedRequestIds + ", overProvisionedRequests=" + overProvisionedRequests + ", underProvisionedRequests=" + underProvisionedRequests + "]";
  }


}
