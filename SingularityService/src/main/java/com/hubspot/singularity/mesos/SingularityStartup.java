package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mesos.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.SingularityTaskTranscoder;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

public class SingularityStartup {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);
  
  private final MesosClient mesosClient;
  private final TaskManager taskManager;
  private final SingularityRackManager rackManager;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final DeployManager deployManager;
  private final SingularityTaskTranscoder taskTranscoder;
  
  private final SingularityLogSupport logSupport;
  private final SingularityScheduler scheduler;
  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  
  @Inject
  public SingularityStartup(MesosClient mesosClient, ObjectMapper objectMapper, SingularityScheduler scheduler, Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityTaskTranscoder taskTranscoder, 
      SingularityHealthchecker healthchecker, SingularityNewTaskChecker newTaskChecker, SingularityRackManager rackManager, TaskManager taskManager, DeployManager deployManager, SingularityLogSupport logSupport) {
    this.mesosClient = mesosClient;
    this.scheduler = scheduler;
    this.stateCacheProvider = stateCacheProvider;
    this.rackManager = rackManager;
    this.deployManager = deployManager;
    this.newTaskChecker = newTaskChecker;
    this.taskManager = taskManager;
    this.healthchecker = healthchecker;
    this.taskTranscoder = taskTranscoder;
    this.logSupport = logSupport;
  }
  
  public void startup(MasterInfo masterInfo) {
    final long start = System.currentTimeMillis();
    
    final String uri = mesosClient.getMasterUri(masterInfo);
    
    LOG.info("Starting up... fetching state data from: " + uri);
    
    try {
      MesosMasterStateObject state = mesosClient.getMasterState(uri);
      
      rackManager.loadRacksFromMaster(state);
      
      // two things need to happen: 
      // 1- we need to look for active tasks that are no longer active (assume that there is no such thing as a missing active task.)
      // 2- we need to reschedule the world.
      
      checkForMissingActiveTasks(state);
      enqueueHealthAndNewTaskchecks();
      
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
    
    LOG.info("Finished startup after {}", JavaUtils.duration(start));
  }
  
  private void enqueueHealthAndNewTaskchecks() {
    final long start = System.currentTimeMillis();
    
    final List<SingularityTask> activeTasks = taskManager.getActiveTasks();
    final Map<SingularityTaskId, SingularityTask> activeTaskMap = Maps.uniqueIndex(activeTasks, taskTranscoder);
    
    final Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> taskUpdates = taskManager.getTaskHistoryUpdates(activeTaskMap.keySet());
    
    final List<SingularityPendingDeploy> pendingDeploys = deployManager.getPendingDeploys();
    
    int enqueuedNewTaskChecks = 0;
    int enqueuedHealthchecks = 0;
    
    for (SingularityTaskId taskId : activeTaskMap.keySet()) {
      SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(taskUpdates.get(taskId));
      SingularityTask task = activeTaskMap.get(taskId);
      
      if (simplifiedTaskState != SimplifiedTaskState.DONE) {
        if (!hasMatchingPendingDeploy(pendingDeploys, taskId)) {
          newTaskChecker.enqueueNewTaskCheck(task);
          enqueuedNewTaskChecks++;
        }
      } else if (simplifiedTaskState == SimplifiedTaskState.RUNNING) {
        if (healthchecker.shouldHealthcheck(task)) {
          healthchecker.enqueueHealthcheck(task);
          enqueuedHealthchecks++;
        }
      }
    }
    
    LOG.info("Enqueued {} health checks and {} new task checks (out of {} active tasks) in {}", enqueuedHealthchecks, enqueuedNewTaskChecks, activeTasks.size(), JavaUtils.duration(start));
  }
  
  private boolean hasMatchingPendingDeploy(List<SingularityPendingDeploy> pendingDeploys, SingularityTaskId taskId) {
    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      if (pendingDeploy.getDeployMarker().getRequestId().equals(taskId.getRequestId()) && pendingDeploy.getDeployMarker().getDeployId().equals(taskId.getDeployId())) {
        return true;
      }
    }
    
    return false;
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
      SingularityTaskId taskId = SingularityTaskId.fromString(strTaskId);
      
      taskManager.saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(taskId, System.currentTimeMillis(), ExtendedTaskState.TASK_LOST_WHILE_DOWN, Optional.<String> absent()));
      
      logSupport.checkDirectory(taskId);
      
      scheduler.handleCompletedTask(taskManager.getActiveTask(strTaskId), taskId, ExtendedTaskState.TASK_LOST_WHILE_DOWN, stateCacheProvider.get());
    }
    
    LOG.info("Finished reconciling active tasks: {} active tasks, {} were deleted", activeTaskIds.size(), strTaskIds.size());
  }
  
}
