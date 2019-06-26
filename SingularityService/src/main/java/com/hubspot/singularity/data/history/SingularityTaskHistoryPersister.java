package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
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
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityTaskHistoryPersister extends SingularityHistoryPersister<SingularityTaskId> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskHistoryPersister.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  private final RequestManager requestManager;
  private final SingularitySchedulerLock lock;
  private final int agentReregisterTimeoutSeconds;

  @Inject
  public SingularityTaskHistoryPersister(SingularityConfiguration configuration, TaskManager taskManager,
                                         DeployManager deployManager, HistoryManager historyManager, @Named(SingularityHistoryModule.PERSISTER_LOCK) ReentrantLock persisterLock,
                                         RequestManager requestManager, SingularitySchedulerLock lock) {
    super(configuration, persisterLock);

    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.lock = lock;
    this.agentReregisterTimeoutSeconds = configuration.getMesosConfiguration().getAgentReregisterTimeoutSeconds();
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Attempting to grab persister lock");
    persisterLock.lock();
    try {
      LOG.info("Checking inactive task ids for task history persistence");

      final long start = System.currentTimeMillis();
      final List<SingularityTaskId> allTaskIds = taskManager.getAllTaskIds();
      allTaskIds.sort(SingularityTaskId.STARTED_AT_COMPARATOR_DESC);

      AtomicInteger numTotal = new AtomicInteger();
      AtomicInteger numTransferred = new AtomicInteger();

      for (String requestId : requestManager.getAllRequestIds()) {
        lock.runWithRequestLock(() -> {
          List<SingularityTaskId> activeForRequest = taskManager.getActiveTaskIdsForRequest(requestId);
          Set<SingularityTaskId> lbCleaningTaskIds = Sets.newHashSet(taskManager.getLBCleanupTasks());
          List<SingularityPendingDeploy> pendingDeploys = deployManager.getPendingDeploys();

          AtomicInteger transferred = new AtomicInteger();
          allTaskIds.stream()
              .filter((t) -> t.getRequestId().equals(requestId))
              .filter((t) -> !(activeForRequest.contains(t) || lbCleaningTaskIds.contains(t) || isPartOfPendingDeploy(pendingDeploys, t) || couldReturnWithRecoveredAgent(t)))
              .forEach((t) -> {
                if (moveToHistoryOrCheckForPurge(t, transferred.getAndIncrement())) {
                  numTransferred.getAndIncrement();
                }

                numTotal.getAndIncrement();
              });


        }, requestId, "task history persister");
      }

      LOG.info("Transferred {} out of {} inactive task ids (total {}) in {}", numTransferred, numTotal, allTaskIds.size(), JavaUtils.duration(start));
    } finally {
      persisterLock.unlock();
    }
  }

  private boolean isPartOfPendingDeploy(List<SingularityPendingDeploy> pendingDeploys, SingularityTaskId taskId) {
    for (SingularityPendingDeploy pendingDeploy : pendingDeploys) {
      if (pendingDeploy.getDeployMarker().getDeployId().equals(taskId.getDeployId()) && pendingDeploy.getDeployMarker().getRequestId().equals(taskId.getRequestId())) {
        return true;
      }
    }

    return false;
  }

  private boolean couldReturnWithRecoveredAgent(SingularityTaskId taskId) {
    Optional<SingularityTaskHistoryUpdate> maybeUnreachable = taskManager.getTaskHistoryUpdate(taskId, ExtendedTaskState.TASK_LOST)
        .or(taskManager.getTaskHistoryUpdate(taskId, ExtendedTaskState.TASK_UNREACHABLE));
    boolean couldReturn = false;
    long lastUpdateTime = 0;
    if (maybeUnreachable.isPresent()) {
      lastUpdateTime = maybeUnreachable.get().getTimestamp();
      if (maybeUnreachable.get().getTaskState() == ExtendedTaskState.TASK_UNREACHABLE) {
        couldReturn = true;
      }
      if (maybeUnreachable.get().getTaskState() == ExtendedTaskState.TASK_LOST
          && maybeUnreachable.get().getStatusReason().isPresent()
          && maybeUnreachable.get().getStatusReason().get().equals(Reason.REASON_AGENT_REMOVED.name())) {
        couldReturn = true;
      }
    }

    // Allow 1.5 times the reregistration timeout before persisting the task
    if (couldReturn) {
      couldReturn = System.currentTimeMillis() - lastUpdateTime < TimeUnit.SECONDS.toMillis(agentReregisterTimeoutSeconds) * 1.5;
    }

    return couldReturn;
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(configuration.getDeleteTasksFromZkWhenNoDatabaseAfterHours());
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxStaleTasksPerRequestInZkWhenNoDatabase();
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
