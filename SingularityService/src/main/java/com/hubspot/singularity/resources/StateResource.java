package com.hubspot.singularity.resources;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.mesos.Protos;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityHostState;
import com.hubspot.singularity.SingularityManaged;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.hooks.WebhookManager;

@Path("/state")
@Produces({ MediaType.APPLICATION_JSON })
public class StateResource {

  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final SingularityManaged managed;
  private final WebhookManager webhookManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  
  @Inject
  public StateResource(RequestManager requestManager, TaskManager taskManager, SingularityManaged managed, WebhookManager webhookManager, SlaveManager slaveManager, RackManager rackManager) {
    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.managed = managed;
    this.webhookManager = webhookManager;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
  }

  @GET
  public SingularityState getState() {
    final boolean isMaster = managed.isMaster();
    final Protos.Status driverStatus = managed.getCurrentStatus();
    
    final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    final long uptime = mxBean.getUptime();
   
    final int activeTasks = taskManager.getNumActiveTasks();
    final int scheduledTasks = taskManager.getNumScheduledTasks();
    final int cleaningTasks = taskManager.getNumCleanupTasks();
    
    final int requests = requestManager.getNumRequests();
    final int pendingRequests = requestManager.getSizeOfPendingQueue();
    final int cleaningRequests = requestManager.getSizeOfCleanupQueue();
    
    final int activeRacks = rackManager.getNumActive();
    final int deadRacks = rackManager.getNumDead();
    final int decomissioningRacks = rackManager.getNumDecomissioning();
    
    final int activeSlaves = slaveManager.getNumActive();
    final int deadSlaves = slaveManager.getNumDead();
    final int decomissioningSlaves = slaveManager.getNumDecomissioning();
    
    final int numWebhooks = webhookManager.getWebhooks().size();
    
    final long now = System.currentTimeMillis();
    final long lastOfferTimestamp = managed.getLastOfferTimestamp();
    final long millisSinceLastOfferTimestamp = now - lastOfferTimestamp;
    
    String hostAddress = null;
    
    try {
      hostAddress = JavaUtils.getHostAddress();
    } catch (Exception e) {
      hostAddress = "Unknown";
    }
        
    final SingularityHostState hostState = new SingularityHostState(isMaster, uptime, driverStatus.name(), millisSinceLastOfferTimestamp, hostAddress, JavaUtils.getHostName());
    
    return new SingularityState(activeTasks, requests, scheduledTasks, pendingRequests, cleaningRequests, driverStatus != null ? driverStatus.name() : "-", activeSlaves, deadSlaves, decomissioningSlaves, activeRacks, deadRacks, decomissioningRacks, numWebhooks, cleaningTasks, Arrays.asList(hostState));
  }
  
}
