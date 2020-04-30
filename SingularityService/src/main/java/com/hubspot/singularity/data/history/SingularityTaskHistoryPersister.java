package com.hubspot.singularity.data.history;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityTaskHistoryPersister
  extends SingularityHistoryPersister<SingularityTaskId> {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityTaskHistoryPersister.class
  );

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  private final int agentReregisterTimeoutSeconds;

  @Inject
  public SingularityTaskHistoryPersister(
    SingularityConfiguration configuration,
    TaskManager taskManager,
    DeployManager deployManager,
    HistoryManager historyManager,
    @Named(SingularityHistoryModule.PERSISTER_LOCK) ReentrantLock persisterLock
  ) {
    super(configuration, persisterLock);
    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.deployManager = deployManager;
    this.agentReregisterTimeoutSeconds =
      configuration.getMesosConfiguration().getAgentReregisterTimeoutSeconds();
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Attempting to grab persister lock");
    persisterLock.lock();
    try {
      LOG.info("Checking inactive task ids for task history persistence");

      final long start = System.currentTimeMillis();
      Map<String, List<SingularityTaskId>> inactiveTaskIdsByRequest = getInactiveTaskIdsByRequest();
      for (Map.Entry<String, List<SingularityTaskId>> entry : inactiveTaskIdsByRequest.entrySet()) {
        int forRequest = 0;
        int transferred = 0;
        for (SingularityTaskId taskId : entry.getValue()) {
          if (moveToHistoryOrCheckForPurge(taskId, forRequest)) {
            LOG.debug("Transferred task {}", taskId);
            transferred++;
          }

          forRequest++;
        }
        LOG.info(
          "Transferred {} out of {} inactive task ids in {}",
          transferred,
          entry.getValue().size(),
          JavaUtils.duration(start)
        );
      }
    } finally {
      persisterLock.unlock();
    }
  }

  private Map<String, List<SingularityTaskId>> getInactiveTaskIdsByRequest() {
    final Set<SingularityTaskId> taskIds = new HashSet<>(taskManager.getAllTaskIds());
    taskIds.removeAll(taskManager.getActiveTaskIds());
    taskIds.removeAll(taskManager.getLBCleanupTasks());
    List<SingularityPendingDeploy> pendingDeploys = deployManager.getPendingDeploys();
    return taskIds
      .stream()
      .filter(
        taskId ->
          !isPartOfPendingDeploy(pendingDeploys, taskId) &&
          !couldReturnWithRecoveredAgent(taskId)
      )
      .sorted(SingularityTaskId.STARTED_AT_COMPARATOR_DESC)
      .collect(Collectors.groupingBy(SingularityTaskId::getRequestId));
  }

  private boolean isPartOfPendingDeploy(
    List<SingularityPendingDeploy> pendingDeploys,
    SingularityTaskId taskId
  ) {
    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      if (
        pendingDeploy.getDeployMarker().getDeployId().equals(taskId.getDeployId()) &&
        pendingDeploy.getDeployMarker().getRequestId().equals(taskId.getRequestId())
      ) {
        return true;
      }
    }

    return false;
  }

  private boolean couldReturnWithRecoveredAgent(SingularityTaskId taskId) {
    Optional<SingularityTaskHistoryUpdate> maybeUnreachable = taskManager.getTaskHistoryUpdate(
      taskId,
      ExtendedTaskState.TASK_LOST
    );
    if (!maybeUnreachable.isPresent()) {
      maybeUnreachable =
        taskManager.getTaskHistoryUpdate(taskId, ExtendedTaskState.TASK_UNREACHABLE);
    }
    boolean couldReturn = false;
    long lastUpdateTime = 0;
    if (maybeUnreachable.isPresent()) {
      lastUpdateTime = maybeUnreachable.get().getTimestamp();
      if (maybeUnreachable.get().getTaskState() == ExtendedTaskState.TASK_UNREACHABLE) {
        couldReturn = true;
      }
      if (
        maybeUnreachable.get().getTaskState() == ExtendedTaskState.TASK_LOST &&
        maybeUnreachable.get().getStatusReason().isPresent() &&
        maybeUnreachable
          .get()
          .getStatusReason()
          .get()
          .equals(Reason.REASON_AGENT_REMOVED.name())
      ) {
        couldReturn = true;
      }
    }

    // Allow 1.5 times the reregistration timeout before persisting the task
    if (couldReturn) {
      couldReturn =
        System.currentTimeMillis() -
        lastUpdateTime <
        TimeUnit.SECONDS.toMillis(agentReregisterTimeoutSeconds) *
        1.5;
    }

    return couldReturn;
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(
      configuration.getDeleteTasksFromZkWhenNoDatabaseAfterHours()
    );
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxStaleTasksPerRequestInZkWhenNoDatabase();
  }

  @Override
  protected boolean moveToHistory(SingularityTaskId object) {
    final Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(
      object
    );

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
