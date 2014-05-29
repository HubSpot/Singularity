package com.hubspot.singularity.mesos;

import java.util.Collections;
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
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskHistoryUpdate.SimplifiedTaskState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.MesosConfiguration;
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
  private final MesosConfiguration mesosConfiguration;
  
  @Inject
  public SingularityStartup(MesosConfiguration mesosConfiguration, MesosClient mesosClient, ObjectMapper objectMapper, SingularityScheduler scheduler, Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityTaskTranscoder taskTranscoder, 
      SingularityHealthchecker healthchecker, SingularityNewTaskChecker newTaskChecker, SingularityRackManager rackManager, TaskManager taskManager, DeployManager deployManager, SingularityLogSupport logSupport, SingularityAbort abort) {
    this.mesosConfiguration = mesosConfiguration;
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
  
  public void startup(MasterInfo masterInfo, boolean registered) {
    final long start = System.currentTimeMillis();
    
    final String uri = mesosClient.getMasterUri(masterInfo);
    
    LOG.info("Starting up... fetching state data from: " + uri);
    
    try {
      MesosMasterStateObject state = mesosClient.getMasterState(uri);
      
      rackManager.loadRacksFromMaster(state);
      
      // two things need to happen: 
      // 1- we need to look for active tasks that are no longer active (assume that there is no such thing as a missing active task.)
      // 2- we need to reschedule the world.
      
      checkForMissingActiveTasks(state, registered);
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
    
    final Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(activeTaskMap.keySet());
    
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
        if (simplifiedTaskState == SimplifiedTaskState.RUNNING) {
          if (healthchecker.shouldHealthcheck(task)) {
            healthchecker.enqueueHealthcheck(task);
            enqueuedHealthchecks++;
          }
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
  
  private void checkForMissingActiveTasks(MesosMasterStateObject state, boolean frameworkIsNew) {    
    final long start = System.currentTimeMillis();
    
    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    
    final Set<String> inactiveTaskIds = Sets.newHashSetWithExpectedSize(activeTaskIds.size());
    for (SingularityTaskId taskId : activeTaskIds) {
      inactiveTaskIds.add(taskId.toString());
    }
    
    List<MesosTaskObject> frameworkRunningTasks = Collections.emptyList();
    
    for (MesosFrameworkObject framework : state.getFrameworks()) {
      if (!framework.getId().equals(mesosConfiguration.getFrameworkId())) {
        LOG.info("Skipping framework {}", framework.getId());
        continue;
      }
    
      frameworkRunningTasks = framework.getTasks();
    }
    
    for (MesosTaskObject taskObject : frameworkRunningTasks) {
      inactiveTaskIds.remove(taskObject.getId());
    }
    
    // we've lost all tasks
    if (frameworkRunningTasks.isEmpty() && !activeTaskIds.isEmpty()) {
      final String msg = String.format("Framework %s (new: %s) had no active tasks, expected ~ %s", mesosConfiguration.getFrameworkId(), frameworkIsNew, activeTaskIds.size());
      
      if (mesosConfiguration.getAllowMissingAllExistingTasksOnStartup().booleanValue()) {
        LOG.info("Ignoring task mismatch because allowMissingAllExistingTasksOnStartup is true");
      } else {
        throw new IllegalStateException(String.format("%s - %s", msg, "set allowMissingAllExistingTasksOnStartup in mesos configuration to true or remove active tasks manually / check framework / zookeeper ids"));
      }
    }
    
    for (String inactiveTaskId : inactiveTaskIds) {
      SingularityTaskId taskId = SingularityTaskId.fromString(inactiveTaskId);
      
      SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(taskId, System.currentTimeMillis(), ExtendedTaskState.TASK_LOST_WHILE_DOWN, Optional.<String> absent()));
      
      logSupport.checkDirectory(taskId);
      
      scheduler.handleCompletedTask(taskManager.getActiveTask(inactiveTaskId), taskId, ExtendedTaskState.TASK_LOST_WHILE_DOWN, taskHistoryUpdateCreateResult, stateCacheProvider.get());
    }
    
    LOG.info("Finished reconciling active tasks: {} active tasks, {} were deleted in {}", activeTaskIds.size(), inactiveTaskIds.size(), JavaUtils.duration(start));
  }
  
}
