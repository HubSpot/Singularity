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
  private final int pendingRequests;
  private final int cleaningRequests;

  @JsonCreator
  public SingularityState(@JsonProperty("master") boolean master, @JsonProperty("uptime") long uptime, @JsonProperty("activeTasks") int activeTasks,
      @JsonProperty("requests") int requests, @JsonProperty("scheduledTasks") int scheduledTasks, @JsonProperty("pendingRequests") int pendingRequests,
      @JsonProperty("cleaningRequests") int cleaningRequests, @JsonProperty("driverStatus") String driverStatus) {
    this.master = master;
    this.uptime = uptime;
    this.activeTasks = activeTasks;
    this.requests = requests;
    this.scheduledTasks = scheduledTasks;
    this.pendingRequests = pendingRequests;
    this.cleaningRequests = cleaningRequests;
    this.driverStatus = driverStatus;
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

}
