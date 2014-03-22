package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;

public class SingularityStartup {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);
  
  private final MesosClient mesosClient;
  private final TaskManager taskManager;
  private final SingularityRackManager rackManager;
  private final RequestManager requestManager;
  private final SingularityHealthchecker healthchecker;
  
  @Inject
  public SingularityStartup(MesosClient mesosClient, ObjectMapper objectMapper, SingularityHealthchecker healthchecker, SingularityRackManager rackManager, TaskManager taskManager, RequestManager requestManager) {
    this.mesosClient = mesosClient;
    this.rackManager = rackManager;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.healthchecker = healthchecker;
  }
  
  public void startup(MasterInfo masterInfo) {
    final String uri = mesosClient.getMasterUri(masterInfo);
    
    final long start = System.currentTimeMillis();
    
    LOG.info("Starting up... fetching state data from: " + uri);
    
    try {
      MesosMasterStateObject state = mesosClient.getMasterState(uri);
      
      rackManager.loadRacksFromMaster(state);
      
      // two things need to happen: 
      // 1- we need to look for active tasks that are no longer active (assume that there is no such thing as a missing active task.)
      // 2- we need to reschedule the world.
      
      checkForMissingActiveTasks(state);
      enqueueHealthchecks();
      rescheduleTheWorld();
      
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
    
    LOG.info(String.format("Finished startup after %sms", System.currentTimeMillis() - start));
  }
  
  private void enqueueHealthchecks() {
    List<SingularityTask> activeTasks = taskManager.getActiveTasks();
    Map<SingularityTaskId, SingularityTask> activeTaskMap = Maps.uniqueIndex(activeTasks, new Function<SingularityTask, SingularityTaskId>() {
      @Override
      public SingularityTaskId apply(SingularityTask input) {
        return input.getTaskId();
      }
    });;
    
    Set<SingularityTaskId> taskIdsToPossiblyHealthcheck = Sets.newHashSetWithExpectedSize(activeTasks.size());
    
    for (SingularityTask activeTask : activeTasks) {
      if (healthchecker.shouldHealthcheck(activeTask)) {
        taskIdsToPossiblyHealthcheck.add(activeTask.getTaskId());
      }
    }
    
    taskIdsToPossiblyHealthcheck.removeAll(taskManager.getHealthcheckResults(taskIdsToPossiblyHealthcheck).keySet());
    
    Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> taskUpdates = taskManager.getTaskHistoryUpdates(taskIdsToPossiblyHealthcheck);
    
    int enqueuedHealthchecks = 0;
    
    for (SingularityTaskId taskId : taskIdsToPossiblyHealthcheck) {
      SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(taskUpdates.get(taskId));
      
      if (simplifiedTaskState == SimplifiedTaskState.RUNNING) {
        healthchecker.enqueueHealthcheck(activeTaskMap.get(taskId));
        enqueuedHealthchecks++;
      }
    }
    
    LOG.info(String.format("Finishing checking for healthchecks, enqueued %s", enqueuedHealthchecks));
  }
  
  private void checkForMissingActiveTasks(MesosMasterStateObject state) {
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final Set<String> strTaskIds = Sets.newHashSetWithExpectedSize(activeTaskIds.size());
    for (SingularityTaskId taskId : activeTaskIds) {
      strTaskIds.add(taskId.toString());
    }
    
    for (MesosFrameworkObject framework : state.getFrameworks()) {
      for (MesosTaskObject taskObject : framework.getTasks()) {
        strTaskIds.remove(taskObject.getId());
      }
    }
    
    // these are no longer running.
    for (String strTaskId : strTaskIds) {
      // TODO record history?
      taskManager.deleteActiveTask(strTaskId);
    }
    
    LOG.info(String.format("Finished reconciling active tasks: %s active tasks, %s were deleted", activeTaskIds.size(), strTaskIds.size()));
  }
  
  private void rescheduleTheWorld() {
    final List<SingularityRequest> requests = requestManager.getActiveRequests();
    
    for (SingularityRequest request : requests) {
      if (!request.isOneOff()) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), Optional.<String> absent(), PendingType.STARTUP));
      }
    }
    
    LOG.info(String.format("Put %s requests into pending queue", requests.size()));
  }
  
}
