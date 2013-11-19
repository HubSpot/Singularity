package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityState {

  private final boolean master;
  private final long uptime;

  private final String driverStatus;
  
  private final int activeTasks;
  private final int requests;
  private final int scheduledTasks;
  private final int cleaningTasks;
  
  private final int pendingRequests;
  private final int cleaningRequests;
  
  private final int activeSlaves;
  private final int deadSlaves;
  private final int decomissioningSlaves;
  
  private final int activeRacks;
  private final int deadRacks;
  private final int decomissioningRacks;
  
  private final int numWebhooks;
  
  private final long millisSinceLastOffer;

  @JsonCreator
  public SingularityState(@JsonProperty("master") boolean master, @JsonProperty("uptime") long uptime, @JsonProperty("activeTasks") int activeTasks,
      @JsonProperty("requests") int requests, @JsonProperty("scheduledTasks") int scheduledTasks, @JsonProperty("pendingRequests") int pendingRequests,
      @JsonProperty("cleaningRequests") int cleaningRequests, @JsonProperty("driverStatus") String driverStatus, 
      @JsonProperty("activeSlaves") int activeSlaves, @JsonProperty("deadSlaves") int deadSlaves, @JsonProperty("decomissioningSlaves") int decomissioningSlaves, 
      @JsonProperty("activeRacks") int activeRacks, @JsonProperty("deadRacks") int deadRacks, @JsonProperty("decomissioningRacks") int decomissioningRacks, 
      @JsonProperty("numWebhooks") int numWebhooks, @JsonProperty("cleaningTasks") int cleaningTasks, @JsonProperty("millisSinceLastOffer") long millisSinceLastOffer) {
    this.master = master;
    this.uptime = uptime;
    this.activeTasks = activeTasks;
    this.requests = requests;
    this.scheduledTasks = scheduledTasks;
    this.pendingRequests = pendingRequests;
    this.cleaningRequests = cleaningRequests;
    this.driverStatus = driverStatus;
    this.activeRacks = activeRacks;
    this.activeSlaves = activeSlaves;
    this.deadRacks = deadRacks;
    this.deadSlaves = deadSlaves;
    this.decomissioningRacks = decomissioningRacks;
    this.decomissioningSlaves = decomissioningSlaves;
    this.numWebhooks = numWebhooks;
    this.cleaningTasks = cleaningTasks;
    this.millisSinceLastOffer = millisSinceLastOffer;
  }

  public long getMillisSinceLastOffer() {
    return millisSinceLastOffer;
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

  public String getDriverStatus() {
    return driverStatus;
  }
  
  public boolean isMaster() {
    return master;
  }

  public long getUptime() {
    return uptime;
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

  @Override
  public String toString() {
    return "SingularityState [master=" + master + ", uptime=" + uptime + ", driverStatus=" + driverStatus + ", activeTasks=" + activeTasks + ", requests=" + requests + ", scheduledTasks=" + scheduledTasks + ", cleaningTasks="
        + cleaningTasks + ", pendingRequests=" + pendingRequests + ", cleaningRequests=" + cleaningRequests + ", activeSlaves=" + activeSlaves + ", deadSlaves=" + deadSlaves + ", decomissioningSlaves=" + decomissioningSlaves
        + ", activeRacks=" + activeRacks + ", deadRacks=" + deadRacks + ", decomissioningRacks=" + decomissioningRacks + ", numWebhooks=" + numWebhooks + ", millisSinceLastOffer=" + millisSinceLastOffer + "]";
  }

}
