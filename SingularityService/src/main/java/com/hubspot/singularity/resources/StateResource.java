package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.*;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path(SingularityService.API_BASE_PATH + "/state")
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource {

  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final StateManager stateManager;
  private final SingularityConfiguration singularityConfiguration;
  
  @Inject
  public StateResource(RequestManager requestManager, DeployManager deployManager, TaskManager taskManager, StateManager stateManager, SlaveManager slaveManager, RackManager rackManager, SingularityConfiguration singularityConfiguration) {
    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.stateManager = stateManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.deployManager = deployManager;
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
        
    final List<SingularityHostState> states = stateManager.getHostStates();
    
    int numDeploys = 0;
    long oldestDeploy = 0;
    final long now = System.currentTimeMillis();
    
    for (SingularityPendingDeploy pendingDeploy : deployManager.getPendingDeploys()) {
      long delta = now - pendingDeploy.getDeployMarker().getTimestamp();
      if (delta > oldestDeploy) {
        oldestDeploy = delta;
      }
      numDeploys++;
    }
    
    return new SingularityState(activeTasks, requests, pausedRequests, scheduledTasks, pendingRequests, cleaningRequests, activeSlaves, deadSlaves, decomissioningSlaves, activeRacks, deadRacks, 
        decomissioningRacks, cleaningTasks, states, oldestDeploy, numDeploys, scheduledTasksInfo.getNumLateTasks(), scheduledTasksInfo.getNumFutureTasks(), scheduledTasksInfo.getMaxTaskLag());
  }
  
}
