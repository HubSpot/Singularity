package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SingularityState {
  
  private final int activeTasks;
  private final int requests;
  private final int pausedRequests;
  private final int scheduledTasks;
  private final int lateTasks;
  private final int futureTasks;
  private final int cleaningTasks;

  private final long maxTaskLag;
  
  private final int pendingRequests;
  private final int cleaningRequests;
  
  private final int activeSlaves;
  private final int deadSlaves;
  private final int decomissioningSlaves;
  
  private final int activeRacks;
  private final int deadRacks;
  private final int decomissioningRacks;
  
  private final int numWebhooks;
  
  private final List<SingularityHostState> hostStates;

  @JsonCreator
  public SingularityState(@JsonProperty("activeTasks") int activeTasks, @JsonProperty("requests") int requests, @JsonProperty("pausedRequests") int pausedRequests, @JsonProperty("scheduledTasks") int scheduledTasks, @JsonProperty("pendingRequests") int pendingRequests,
      @JsonProperty("cleaningRequests") int cleaningRequests, @JsonProperty("activeSlaves") int activeSlaves, @JsonProperty("deadSlaves") int deadSlaves, 
      @JsonProperty("decomissioningSlaves") int decomissioningSlaves, @JsonProperty("activeRacks") int activeRacks, @JsonProperty("deadRacks") int deadRacks, @JsonProperty("decomissioningRacks") int decomissioningRacks, 
      @JsonProperty("numWebhooks") int numWebhooks, @JsonProperty("cleaningTasks") int cleaningTasks, @JsonProperty("hostStates") List<SingularityHostState> hostStates,
      @JsonProperty("lateTasks") int lateTasks, @JsonProperty("futureTasks") int futureTasks, @JsonProperty("maxTaskLag") long maxTaskLag) {
    this.activeTasks = activeTasks;
    this.requests = requests;
    this.pausedRequests = pausedRequests;
    this.scheduledTasks = scheduledTasks;
    this.pendingRequests = pendingRequests;
    this.cleaningRequests = cleaningRequests;
    this.activeRacks = activeRacks;
    this.activeSlaves = activeSlaves;
    this.deadRacks = deadRacks;
    this.deadSlaves = deadSlaves;
    this.decomissioningRacks = decomissioningRacks;
    this.decomissioningSlaves = decomissioningSlaves;
    this.numWebhooks = numWebhooks;
    this.cleaningTasks = cleaningTasks;
    this.hostStates = hostStates;
    this.lateTasks = lateTasks;
    this.futureTasks = futureTasks;
    this.maxTaskLag = maxTaskLag;
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

  public int getNumWebhooks() {
    return numWebhooks;
  }
  
  public int getActiveTasks() {
    return activeTasks;
  }

  public int getRequests() {
    return requests;
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

  @Override
  public String toString() {
    return "SingularityState [activeTasks=" + activeTasks + ", requests=" + requests + ", pausedRequests=" + pausedRequests + ", scheduledTasks=" + scheduledTasks + ", cleaningTasks=" + cleaningTasks + ", pendingRequests="
        + pendingRequests + ", cleaningRequests=" + cleaningRequests + ", activeSlaves=" + activeSlaves + ", deadSlaves=" + deadSlaves + ", decomissioningSlaves=" + decomissioningSlaves + ", activeRacks=" + activeRacks + ", deadRacks="
        + deadRacks + ", decomissioningRacks=" + decomissioningRacks + ", numWebhooks=" + numWebhooks + ", hostStates=" + hostStates + ", lateTasks=" + lateTasks + ", futureTasks=" + futureTasks + ", maxTaskLag=" + maxTaskLag + "]";
  }

}
