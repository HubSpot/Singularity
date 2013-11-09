package com.hubspot.singularity.resources;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.mesos.Protos;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityManaged;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Path("/state")
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource {

  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final SingularityManaged managed;
  
  @Inject
  public StateResource(RequestManager requestManager, TaskManager taskManager, SingularityManaged managed) {
    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.managed = managed;
  }

  @GET
  public SingularityState getState() {
    final boolean isMaster = managed.isMaster();
    final Protos.Status driverStatus = managed.getCurrentStatus();
    
    final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    final long uptime = mxBean.getUptime();
    final long startTime = System.currentTimeMillis() - uptime;
    
    final int activeTasks = taskManager.getNumActiveTasks();
    final int scheduledTasks = taskManager.getNumScheduledTasks();
    
    final int requests = requestManager.getNumRequests();
    final int pendingRequests = requestManager.getSizeOfPendingQueue();
    final int cleaningRequests = requestManager.getSizeOfCleanupQueue();
    
    return new SingularityState(isMaster, startTime, activeTasks, requests, scheduledTasks, pendingRequests, cleaningRequests, driverStatus != null ? driverStatus.name() : "-");
  }
  
}
