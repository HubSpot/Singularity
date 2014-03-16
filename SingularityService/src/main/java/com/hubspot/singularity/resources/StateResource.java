package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityHostState;
import com.hubspot.singularity.SingularityScheduledTasksInfo;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.WebhookManager;

@Path("/state")
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource {

  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final WebhookManager webhookManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final StateManager stateManager;
  private final SingularityConfiguration singularityConfiguration;
  
  @Inject
  public StateResource(RequestManager requestManager, TaskManager taskManager, StateManager stateManager, WebhookManager webhookManager, SlaveManager slaveManager, RackManager rackManager, SingularityConfiguration singularityConfiguration) {
    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.stateManager = stateManager;
    this.webhookManager = webhookManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.singularityConfiguration = singularityConfiguration;
  }

  @GET
  public SingularityState getState() {
    final int activeTasks = taskManager.getNumActiveTasks();
    final int scheduledTasks = taskManager.getNumScheduledTasks();
    final int cleaningTasks = taskManager.getNumCleanupTasks();

    final SingularityScheduledTasksInfo scheduledTasksInfo = SingularityScheduledTasksInfo.getInfo(taskManager.getScheduledTasks(), singularityConfiguration.getDeltaAfterWhichTasksAreLateMillis());
    
    final int requests = requestManager.getNumRequests();
    final int pendingRequests = requestManager.getSizeOfPendingQueue();
    final int cleaningRequests = requestManager.getSizeOfCleanupQueue();
    final int pausedRequests = requestManager.getNumPausedRequests();
    
    final int activeRacks = rackManager.getNumActive();
    final int deadRacks = rackManager.getNumDead();
    final int decomissioningRacks = rackManager.getNumDecomissioning();
    
    final int activeSlaves = slaveManager.getNumActive();
    final int deadSlaves = slaveManager.getNumDead();
    final int decomissioningSlaves = slaveManager.getNumDecomissioning();
    
    final int numWebhooks = webhookManager.getWebhooks().size();
    
    final List<SingularityHostState> states = stateManager.getHostStates();
    
    return new SingularityState(activeTasks, requests, pausedRequests, scheduledTasks, pendingRequests, cleaningRequests, activeSlaves, deadSlaves, decomissioningSlaves, activeRacks, deadRacks, decomissioningRacks, numWebhooks, cleaningTasks, states, scheduledTasksInfo.getNumLateTasks(), scheduledTasksInfo.getNumFutureTasks(), scheduledTasksInfo.getMaxTaskLag());
  }
  
}
