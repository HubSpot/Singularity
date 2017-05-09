package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.config.HistoryPurgeRequestSettings;
import com.hubspot.singularity.config.HistoryPurgingConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;

@Singleton
public class SingularityHistoryPurger extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHistoryPurger.class);

  private final HistoryPurgingConfiguration historyPurgingConfiguration;
  private final HistoryManager historyManager;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final MetadataManager metadataManager;

  @Inject
  public SingularityHistoryPurger(HistoryPurgingConfiguration historyPurgingConfiguration, HistoryManager historyManager,
                                  TaskManager taskManager, DeployManager deployManager, RequestManager requestManager, MetadataManager metadataManager) {
    super(historyPurgingConfiguration.getCheckTaskHistoryEveryHours(), TimeUnit.HOURS);

    this.historyPurgingConfiguration = historyPurgingConfiguration;
    this.historyManager = historyManager;
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.metadataManager = metadataManager;
  }

  @Override
  protected boolean isEnabled() {
    return historyPurgingConfiguration.isEnabledAndValid();
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();
    for (String requestId : historyManager.getRequestIdsInTaskHistory()) {
      HistoryPurgeRequestSettings settings = getRequestPurgeSettings(requestId);

      LOG.debug("Attempting to purge tasks for {}, using purge settings {}", requestId, settings);
      if (settings.getDeleteTaskHistoryAfterTasksPerRequest().isPresent() || settings.getDeleteTaskHistoryAfterDays().isPresent()) {
        purge(requestId, start, settings.getDeleteTaskHistoryAfterTasksPerRequest(), settings.getDeleteTaskHistoryAfterDays(), true);
      } else {
        LOG.debug("No purge settings for deleting task row, skipping for request {}", requestId);
      }
      if (settings.getDeleteTaskHistoryBytesAfterTasksPerRequest().isPresent() || settings.getDeleteTaskHistoryBytesAfterDays().isPresent()) {
        purge(requestId, start, settings.getDeleteTaskHistoryBytesAfterTasksPerRequest(), settings.getDeleteTaskHistoryBytesAfterDays(), false);
      } else {
        LOG.debug("No purge settings for removing task bytes, skipping for request {}", requestId);
      }
    }
    purgeStaleZkData();
  }

  private void purge(String requestId, long start, Optional<Integer> afterTasksPerRequest, Optional<Integer> afterDays, boolean deleteRow) {
    Optional<Date> purgeBefore = Optional.absent();
    Date checkBefore = new Date();

    if (afterDays.isPresent()) {
      purgeBefore = Optional.of(new Date(start - TimeUnit.DAYS.toMillis(afterDays.get().longValue())));

      if (!afterTasksPerRequest.isPresent()) {
        checkBefore = purgeBefore.get();
      }
    }

    LOG.info("Finding taskHistory counts before {} (purging tasks over limit of {} or created before {}) for request {}", checkBefore, afterTasksPerRequest, purgeBefore, requestId);

    int unpurgedCount;
    if (deleteRow) {
      unpurgedCount = historyManager.getTaskIdHistoryCount(Optional.of(requestId), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<ExtendedTaskState>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent(), Optional.<Long>absent());
    } else {
      unpurgedCount = historyManager.getUnpurgedTaskHistoryCountByRequestBefore(requestId, checkBefore);
    }

    if (!afterDays.isPresent() && afterTasksPerRequest.isPresent() &&
      unpurgedCount < afterTasksPerRequest.get()) {
      LOG.debug("Not purging old taskHistory for {} - {} count is less than {}", requestId, unpurgedCount, afterTasksPerRequest.get());
      return;
    }

    final long startRequestId = System.currentTimeMillis();

    historyManager.purgeTaskHistory(requestId, unpurgedCount, afterTasksPerRequest, purgeBefore, deleteRow, historyPurgingConfiguration.getPurgeLimitPerQuery());

    LOG.info("Purged old taskHistory for {} ({} count) in {} (deleteRows: {})", requestId, unpurgedCount, JavaUtils.duration(startRequestId), deleteRow);
  }

  private HistoryPurgeRequestSettings getRequestPurgeSettings(String requestId) {
    if (historyPurgingConfiguration.getRequestOverrides().containsKey(requestId)) {
      HistoryPurgeRequestSettings override = historyPurgingConfiguration.getRequestOverrides().get(requestId);
      if (!override.getDeleteTaskHistoryAfterDays().isPresent()) {
        override.setDeleteTaskHistoryAfterDays(historyPurgingConfiguration.getDeleteTaskHistoryAfterDays());
      }
      if (!override.getDeleteTaskHistoryAfterTasksPerRequest().isPresent()) {
        override.setDeleteTaskHistoryAfterTasksPerRequest(historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest());
      }
      if (!override.getDeleteTaskHistoryBytesAfterDays().isPresent()) {
        override.setDeleteTaskHistoryBytesAfterDays(historyPurgingConfiguration.getDeleteTaskHistoryBytesAfterDays());
      }
      if (!override.getDeleteTaskHistoryBytesAfterTasksPerRequest().isPresent()) {
        override.setDeleteTaskHistoryBytesAfterTasksPerRequest(historyPurgingConfiguration.getDeleteTaskHistoryBytesAfterTasksPerRequest());
      }
      return override;
    } else {
      HistoryPurgeRequestSettings settings = new HistoryPurgeRequestSettings();
      settings.setDeleteTaskHistoryAfterDays(historyPurgingConfiguration.getDeleteTaskHistoryAfterDays());
      settings.setDeleteTaskHistoryAfterTasksPerRequest(historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest());
      settings.setDeleteTaskHistoryBytesAfterDays(historyPurgingConfiguration.getDeleteTaskHistoryBytesAfterDays());
      settings.setDeleteTaskHistoryBytesAfterTasksPerRequest(historyPurgingConfiguration.getDeleteTaskHistoryBytesAfterTasksPerRequest());
      return settings;
    }
  }

  private void purgeStaleZkData() {
    try {
      List<String> activeRequestIds = requestManager.getAllRequestIds();
      long deleteBeforeTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(historyPurgingConfiguration.getPurgeStaleRequestIdsAfterDays());
      taskManager.purgeStaleRequests(activeRequestIds, deleteBeforeTime);
      deployManager.purgeStaleRequests(activeRequestIds, deleteBeforeTime);
      metadataManager.purgeStaleRequests(activeRequestIds, deleteBeforeTime);
    } catch (Exception e) {
      LOG.error("Could not purge stale zk data", e);
    }
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

}
