package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.SingularityTaskMetadataConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityTaskHistoryPersister extends SingularityHistoryPersister<SingularityTaskId> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityTaskHistoryPersister.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  private final SingularityTaskMetadataConfiguration taskMetadataConfiguration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public SingularityTaskHistoryPersister(SingularityConfiguration configuration, SingularityTaskMetadataConfiguration taskMetadataConfiguration, TaskManager taskManager,
      DeployManager deployManager, HistoryManager historyManager, SingularityExceptionNotifier exceptionNotifier) {
    super(configuration);

    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.deployManager = deployManager;
    this.taskMetadataConfiguration = taskMetadataConfiguration;
    this.exceptionNotifier = exceptionNotifier;
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

    final Multimap<String, SingularityTaskId> eligibleTaskIdByRequestId = TreeMultimap.create(Ordering.natural(), SingularityTaskId.STARTED_AT_COMPARATOR_DESC);

    for (SingularityTaskId taskId : allTaskIds) {
      if (activeTaskIds.contains(taskId) || lbCleaningTaskIds.contains(taskId) || isPartOfPendingDeploy(pendingDeploys, taskId)) {
        continue;
      }

      eligibleTaskIdByRequestId.put(taskId.getRequestId(), taskId);
    }

    for (Map.Entry<String, Collection<SingularityTaskId>> entry : eligibleTaskIdByRequestId.asMap().entrySet()) {
      int i = 0;
      for (SingularityTaskId taskId : entry.getValue()) {
        final long age = start - taskId.getStartedAt();

        if (age < configuration.getTaskPersistAfterStartupBufferMillis()) {
          LOG.debug("Not persisting {}, it has started up too recently {} (buffer: {}) - this prevents race conditions with ZK tx", taskId, JavaUtils.durationFromMillis(age),
              JavaUtils.durationFromMillis(configuration.getTaskPersistAfterStartupBufferMillis()));
          continue;
        }

        if (moveToHistoryOrCheckForPurge(taskId, i++)) {
          numTransferred++;
        }

        numTotal++;
      }
    }

    LOG.info("Transferred {} out of {} inactive task ids (total {}) in {}", numTransferred, numTotal, allTaskIds.size(), JavaUtils.duration(start));
  }

  private boolean isPartOfPendingDeploy(List<SingularityPendingDeploy> pendingDeploys, SingularityTaskId taskId) {
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
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxStaleTasksPerRequestInZkWhenNoDatabase();
  }

  @Override
  protected boolean moveToHistory(SingularityTaskId object) {
    final Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(object);

    if (taskHistory.isPresent()) {
      if (!taskHistory.get().getTaskUpdates().isEmpty()) {
        final long lastUpdateAt = taskHistory.get().getLastTaskUpdate().get().getTimestamp();

        final long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateAt;

        if (timeSinceLastUpdate < taskMetadataConfiguration.getTaskPersistAfterFinishBufferMillis()) {
          LOG.debug("Not persisting {} yet - lastUpdate only happened {} ago, buffer {}", JavaUtils.durationFromMillis(timeSinceLastUpdate),
              JavaUtils.durationFromMillis(taskMetadataConfiguration.getTaskPersistAfterFinishBufferMillis()));
          return false;
        }
      }

      LOG.debug("Moving {} to history", object);
      try {
        historyManager.saveTaskHistory(taskHistory.get());
      } catch (Throwable t) {
        LOG.warn("Failed to persist task into History for task {}", object, t);
        exceptionNotifier.notify("Failed to persist task history", t);
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
