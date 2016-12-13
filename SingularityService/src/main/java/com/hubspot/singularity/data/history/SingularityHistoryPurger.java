package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.HistoryPurgeRequestOverride;
import com.hubspot.singularity.config.HistoryPurgingConfiguration;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;

@Singleton
public class SingularityHistoryPurger extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityHistoryPurger.class);

  private final HistoryPurgingConfiguration historyPurgingConfiguration;
  private final HistoryManager historyManager;

  @Inject
  public SingularityHistoryPurger(HistoryPurgingConfiguration historyPurgingConfiguration, HistoryManager historyManager) {
    super(historyPurgingConfiguration.getCheckTaskHistoryEveryHours(), TimeUnit.HOURS);

    this.historyPurgingConfiguration = historyPurgingConfiguration;
    this.historyManager = historyManager;
  }

  @Override
  protected boolean isEnabled() {
    return historyPurgingConfiguration.isEnabledAndValid();
  }

  @Override
  public void runActionOnPoll() {
    final long start = System.currentTimeMillis();
    for (String requestId : historyManager.getRequestIdsInTaskHistory()) {
      Optional<Integer> deleteTaskHistoryAfterDays;
      Optional<Integer> deleteTaskHistoryAfterTasksPerRequest;
      boolean deleteTaskHistoryBytesInsteadOfEntireRow;
      if (historyPurgingConfiguration.getRequestOverrides().containsKey(requestId)) {
        HistoryPurgeRequestOverride override = historyPurgingConfiguration.getRequestOverrides().get(requestId);
        deleteTaskHistoryAfterDays = override.getDeleteTaskHistoryAfterDays().or(historyPurgingConfiguration.getDeleteTaskHistoryAfterDays());
        deleteTaskHistoryAfterTasksPerRequest = override.getDeleteTaskHistoryAfterTasksPerRequest().or(historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest());
        deleteTaskHistoryBytesInsteadOfEntireRow = override.getDeleteTaskHistoryBytesInsteadOfEntireRow().or(historyPurgingConfiguration.isDeleteTaskHistoryBytesInsteadOfEntireRow());
      } else {
        deleteTaskHistoryAfterDays = historyPurgingConfiguration.getDeleteTaskHistoryAfterDays();
        deleteTaskHistoryAfterTasksPerRequest = historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest();
        deleteTaskHistoryBytesInsteadOfEntireRow = historyPurgingConfiguration.isDeleteTaskHistoryBytesInsteadOfEntireRow();
      }

      Optional<Date> purgeBefore = Optional.absent();
      Date checkBefore = new Date();

      if (deleteTaskHistoryAfterDays.isPresent()) {
        purgeBefore = Optional.of(new Date(start - TimeUnit.DAYS.toMillis(deleteTaskHistoryAfterDays.get().longValue())));

        if (!deleteTaskHistoryAfterTasksPerRequest.isPresent()) {
          checkBefore = purgeBefore.get();
        }
      }

      LOG.info("Finding taskHistory counts before {} (purging tasks over limit of {} or created before {}) for request {}", checkBefore, deleteTaskHistoryAfterTasksPerRequest, purgeBefore, requestId);

      int unpurgedCount = historyManager.getUnpurgedTaskHistoryCountByRequestBefore(requestId, checkBefore);

      if (!deleteTaskHistoryAfterDays.isPresent() && deleteTaskHistoryAfterTasksPerRequest.isPresent() &&
        unpurgedCount < deleteTaskHistoryAfterTasksPerRequest.get()) {
        LOG.debug("Not purging old taskHistory for {} - {} count is less than {}", requestId, unpurgedCount, deleteTaskHistoryAfterTasksPerRequest.get());
        continue;
      }

      final long startRequestId = System.currentTimeMillis();

      historyManager.purgeTaskHistory(requestId, unpurgedCount, deleteTaskHistoryAfterTasksPerRequest, purgeBefore, !deleteTaskHistoryBytesInsteadOfEntireRow);

      LOG.info("Purged old taskHistory for {} ({} count) in {}", requestId, unpurgedCount, JavaUtils.duration(startRequestId));
    }
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

}
