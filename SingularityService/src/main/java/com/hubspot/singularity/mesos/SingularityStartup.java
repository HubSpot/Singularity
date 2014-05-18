package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Set;

import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.TaskID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityStartup {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);
  
  private final MesosClient mesosClient;
  private final TaskManager taskManager;
  private final SingularityRackManager rackManager;
  private final RequestManager requestManager;
  
  @Inject
  public SingularityStartup(MesosClient mesosClient, ObjectMapper objectMapper, SingularityRackManager rackManager, TaskManager taskManager, RequestManager requestManager) {
    this.mesosClient = mesosClient;
    this.rackManager = rackManager;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
  }
  
  public void startup(MasterInfo masterInfo, SchedulerDriver driver) {
    final String uri = mesosClient.getMasterUri(masterInfo);
    
    final long start = System.currentTimeMillis();
    
    LOG.info("Starting up... fetching state data from: " + uri);
    
    try {
      MesosMasterStateObject state = mesosClient.getMasterState(uri);
      
      rackManager.loadRacksFromMaster(state);
      
      // two things need to happen: 
      // 1- we need to look for active tasks that are no longer active (assume that there is no such thing as a missing active task.)
      // 2- we need to reschedule the world.
      
      checkForMissingActiveTasks(state, driver);
      rescheduleTheWorld();
      
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
    
    LOG.info(String.format("Finished startup after %sms", System.currentTimeMillis() - start));
  }
  
  private void checkForMissingActiveTasks(MesosMasterStateObject state, SchedulerDriver driver) {
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final Set<String> strTaskIds = Sets.newHashSetWithExpectedSize(activeTaskIds.size());
    final List<String> unknownActiveTaskIds = Lists.newArrayList();
    
    for (SingularityTaskId taskId : activeTaskIds) {
      strTaskIds.add(taskId.toString());
    }
    
    for (MesosFrameworkObject framework : state.getFrameworks()) {
      for (MesosTaskObject taskObject : framework.getTasks()) {
        if (!strTaskIds.remove(taskObject.getId())) {
          // wasn't in our active task ids!
          unknownActiveTaskIds.add(taskObject.getId());
        }
      }
    }
    
    // these are no longer running.
    for (String strTaskId : strTaskIds) {
      // TODO record history?
      taskManager.deleteActiveTask(strTaskId);
    }
    
    for (String unknownTaskId : unknownActiveTaskIds) {
      LOG.info("Killing: " + unknownTaskId);
      driver.killTask(TaskID.newBuilder().setValue(unknownTaskId).build());
    }
  
    LOG.info(String.format("Finished reconciling active tasks: %s active tasks, %s were deleted", activeTaskIds.size(), strTaskIds.size()));
  }
  
  private void rescheduleTheWorld() {
    final List<SingularityRequest> requests = requestManager.getActiveRequests();
    
    for (SingularityRequest request : requests) {
      if (!request.isOneOff()) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), PendingType.STARTUP));
      }
    }
    
    LOG.info(String.format("Put %s requests into pending queue", requests.size()));
  }
  
}
