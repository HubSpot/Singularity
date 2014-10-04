package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.TaskState;
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
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosFrameworkObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityStartable;
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

  private static final Logger LOG = LoggerFactory.getLogger(SingularityStartup.class);

  private final MesosClient mesosClient;
  private final TaskManager taskManager;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final DeployManager deployManager;
  private final SingularityTaskTranscoder taskTranscoder;

  private final SingularityLogSupport logSupport;
  private final SingularityScheduler scheduler;
  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final MesosConfiguration mesosConfiguration;

  private final List<SingularityStartable> startables;

  @Inject
  public SingularityStartup(MesosConfiguration mesosConfiguration, MesosClient mesosClient, ObjectMapper objectMapper, SingularityScheduler scheduler, List<SingularityStartable> startables, Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityTaskTranscoder taskTranscoder,
      SingularityHealthchecker healthchecker, SingularityNewTaskChecker newTaskChecker, SingularitySlaveAndRackManager slaveAndRackManager, TaskManager taskManager, DeployManager deployManager, SingularityLogSupport logSupport, SingularityAbort abort) {
    this.mesosConfiguration = mesosConfiguration;
    this.mesosClient = mesosClient;
    this.scheduler = scheduler;
    this.stateCacheProvider = stateCacheProvider;
    this.slaveAndRackManager = slaveAndRackManager;
    this.deployManager = deployManager;
    this.newTaskChecker = newTaskChecker;
    this.taskManager = taskManager;
    this.healthchecker = healthchecker;
    this.taskTranscoder = taskTranscoder;
    this.logSupport = logSupport;
    this.startables = startables;
  }

  public void startup(MasterInfo masterInfo, boolean registered) {
    final long start = System.currentTimeMillis();

    final String uri = mesosClient.getMasterUri(masterInfo);

    LOG.info("Starting up... fetching state data from: " + uri);

    try {
      MesosMasterStateObject state = mesosClient.getMasterState(uri);

      slaveAndRackManager.loadSlavesAndRacksFromMaster(state);

      checkForCompletedActiveTasks(state);
      enqueueHealthAndNewTaskchecks();

    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    startStartables();

    LOG.info("Finished startup after {}", JavaUtils.duration(start));
  }

  private void startStartables() {
    for (SingularityStartable startable : startables) {
      startable.start();
    }
  }

  private void enqueueHealthAndNewTaskchecks() {
    final long start = System.currentTimeMillis();

    final List<SingularityTask> activeTasks = taskManager.getActiveTasks();
    final Map<SingularityTaskId, SingularityTask> activeTaskMap = Maps.uniqueIndex(activeTasks, taskTranscoder);

    final Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> taskUpdates = taskManager.getTaskHistoryUpdates(activeTaskMap.keySet());

    final Map<SingularityDeployKey, SingularityPendingDeploy> pendingDeploys = Maps.uniqueIndex(deployManager.getPendingDeploys(), SingularityDeployKey.FROM_PENDING_TO_DEPLOY_KEY);

    int enqueuedNewTaskChecks = 0;
    int enqueuedHealthchecks = 0;

    for (Map.Entry<SingularityTaskId, SingularityTask> entry: activeTaskMap.entrySet()) {
      SingularityTaskId taskId = entry.getKey();
      SingularityTask task = entry.getValue();
      SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(taskUpdates.get(taskId));

      if (simplifiedTaskState != SimplifiedTaskState.DONE) {
        SingularityDeployKey deployKey = new SingularityDeployKey(taskId.getRequestId(), taskId.getDeployId());
        Optional<SingularityPendingDeploy> pendingDeploy = Optional.fromNullable(pendingDeploys.get(deployKey));

        if (!pendingDeploy.isPresent()) {
          newTaskChecker.enqueueNewTaskCheck(task);
          enqueuedNewTaskChecks++;
        }
        if (simplifiedTaskState == SimplifiedTaskState.RUNNING) {
          if (healthchecker.enqueueHealthcheck(task, pendingDeploy)) {
            enqueuedHealthchecks++;
          }
        }
      }
    }

    LOG.info("Enqueued {} health checks and {} new task checks (out of {} active tasks) in {}", enqueuedHealthchecks, enqueuedNewTaskChecks, activeTasks.size(), JavaUtils.duration(start));
  }

  private void checkForCompletedActiveTasks(MesosMasterStateObject state) {
    final long start = System.currentTimeMillis();

    final Set<SingularityTaskId> activeTaskIds = Sets.newHashSet(taskManager.getActiveTaskIds());

    List<MesosTaskObject> frameworkRunningTasks = Collections.emptyList();

    for (MesosFrameworkObject framework : state.getFrameworks()) {
      if (!framework.getId().equals(mesosConfiguration.getFrameworkId())) {
        LOG.info("Skipping framework {}", framework.getId());
        continue;
      }

      frameworkRunningTasks = framework.getTasks();
    }

    int completedTasks = 0;

    for (MesosTaskObject taskObject : frameworkRunningTasks) {
      SingularityTaskId taskId = SingularityTaskId.fromString(taskObject.getId());

      if (!activeTaskIds.contains(taskId)) {
        continue;
      }

      final TaskState taskState = TaskState.valueOf(taskObject.getState());

      if (MesosUtils.isTaskDone(taskState)) {
        completedTasks++;

        sendTaskUpdate(taskId, ExtendedTaskState.fromTaskState(taskState), start);
      }
    }

    LOG.info("Finished reconciling active tasks: {} active tasks, {} were completed in {}", activeTaskIds.size(), completedTasks, JavaUtils.duration(start));
  }

  private void sendTaskUpdate(SingularityTaskId taskId, ExtendedTaskState extendedTaskState, final long start) {
    SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(taskId, System.currentTimeMillis(), extendedTaskState, Optional.<String> absent()));

    logSupport.checkDirectory(taskId);

    scheduler.handleCompletedTask(taskManager.getActiveTask(taskId.getId()), taskId, start, extendedTaskState, taskHistoryUpdateCreateResult, stateCacheProvider.get());
  }

}
