package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Set;

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
public class SingularityTaskHistoryPersister extends SingularityHistoryPersister {

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
    LOG.info("Checking inactive task ids for task history persistance");

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
      if (transferToHistoryDB(taskId)) {
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

  private boolean transferToHistoryDB(SingularityTaskId inactiveTaskId) {
    final long start = System.currentTimeMillis();

    final Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(inactiveTaskId);

    if (taskHistory.isPresent()) {
      try {
        historyManager.saveTaskHistory(taskHistory.get());
      } catch (Throwable t) {
        LOG.warn("Failed to persist task into History for task {}", inactiveTaskId, t);
        return false;
      }
    } else {
      LOG.warn("Inactive task {} did not have a task to persist", inactiveTaskId);
    }

    SingularityDeleteResult deleteResult = taskManager.deleteTaskHistory(inactiveTaskId);

    LOG.debug("Moved task history for {} from ZK to History in (delete result: {}) in {}", inactiveTaskId, deleteResult, JavaUtils.duration(start));

    return true;
  }

}
