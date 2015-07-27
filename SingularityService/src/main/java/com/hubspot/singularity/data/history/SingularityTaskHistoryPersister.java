package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityTaskHistoryPersister extends SingularityHistoryPersister<SingularityTaskId> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskHistoryPersister.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final HistoryManager historyManager;

  @Inject
  public SingularityTaskHistoryPersister(SingularityConfiguration configuration, TaskManager taskManager, DeployManager deployManager, HistoryManager historyManager) {
    super(configuration);

    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.deployManager = deployManager;
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Checking inactive task ids for task history persistence");

    final long start = System.currentTimeMillis();

    final List<SingularityTaskId> allTaskIds = taskManager.getAllTaskIds();

    final Set<SingularityTaskId> activeTaskIds = Sets.newHashSet(taskManager.getActiveTaskIds());
    final Set<SingularityTaskId> lbCleaningTaskIds = Sets.newHashSet(taskManager.getLBCleanupTasks());
    final List<SingularityPendingDeploy> pendingDeploys = deployManager.getPendingDeploys();

    int numTotal = 0;
    int numTransferred = 0;

    for (SingularityTaskId taskId : allTaskIds) {
      if (activeTaskIds.contains(taskId) || lbCleaningTaskIds.contains(taskId) || isPartofPendingDeploy(pendingDeploys, taskId)) {
        continue;
      }
      if (moveToHistoryOrCheckForPurge(taskId)) {
        numTransferred++;
      }
      numTotal++;
    }

    LOG.info("Transferred {} out of {} inactive task ids (total {}) in {}", numTransferred, numTotal, allTaskIds.size(), JavaUtils.duration(start));
  }

  private boolean isPartofPendingDeploy(List<SingularityPendingDeploy> pendingDeploys, SingularityTaskId taskId) {
    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      if (pendingDeploy.getDeployMarker().getDeployId().equals(taskId.getDeployId()) && pendingDeploy.getDeployMarker().getRequestId().equals(taskId.getRequestId())) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(configuration.getDeleteTasksFromZkWhenNoDatabaseAfterHours());
  }

  @Override
  protected boolean moveToHistory(SingularityTaskId object) {
    final Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(object);

    if (taskHistory.isPresent()) {
      LOG.debug("Moving {} to history", object);
      try {
        historyManager.saveTaskHistory(taskHistory.get());
      } catch (Throwable t) {
        LOG.warn("Failed to persist task into History for task {}", object, t);
        return false;
      }
    } else {
      LOG.warn("Inactive task {} did not have a task to persist", object);
    }

    return true;
  }

  @Override
  protected SingularityDeleteResult purgeFromZk(SingularityTaskId object) {
    return taskManager.deleteTaskHistory(object);
  }

}
