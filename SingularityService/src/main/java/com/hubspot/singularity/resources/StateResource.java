package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityHostState;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityScheduledTasksInfo;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.data.TaskManager;

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
    
    final List<SingularityRequestWithState> requests = requestManager.getRequests();
    
    int numActiveRequests = 0;
    int numPausedRequests = 0;
    int cooldownRequests = 0;

    for (SingularityRequestWithState requestWithState : requests) {
      switch (requestWithState.getState()) {
      case ACTIVE:
        numActiveRequests++;
        break;
      case PAUSED:
        numPausedRequests++;
        break;
      case SYSTEM_COOLDOWN:
        cooldownRequests++;
        break;
      default:
      }
    }
    
    final int pendingRequests = requestManager.getSizeOfPendingQueue();
    final int cleaningRequests = requestManager.getSizeOfCleanupQueue();
    
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
    
    return new SingularityState(activeTasks, numActiveRequests, cooldownRequests, numPausedRequests, scheduledTasks, pendingRequests, cleaningRequests, activeSlaves, deadSlaves, decomissioningSlaves, activeRacks, deadRacks, 
        decomissioningRacks, cleaningTasks, states, oldestDeploy, numDeploys, scheduledTasksInfo.getNumLateTasks(), scheduledTasksInfo.getNumFutureTasks(), scheduledTasksInfo.getMaxTaskLag());
  }
  
}
