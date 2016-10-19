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
import com.hubspot.singularity.config.HistoryPurgingConfiguration;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;
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
    Optional<Date> purgeBefore = Optional.absent();
    Date checkBefore = new Date();

    if (historyPurgingConfiguration.getDeleteTaskHistoryAfterDays().isPresent()) {
      purgeBefore = Optional.of(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(historyPurgingConfiguration.getDeleteTaskHistoryAfterDays().get().longValue())));

      if (!historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest().isPresent()) {
        checkBefore = purgeBefore.get();
      }

      LOG.debug("Purging taskHistory before {}", purgeBefore.get());
    }

    final long start = System.currentTimeMillis();

    LOG.info("Finding taskHistory counts before {} (purging tasks over limit of {})", checkBefore, historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest());

    final List<SingularityRequestIdCount> requestIdCounts = historyManager.getRequestIdCounts(checkBefore);

    LOG.info("Found {} counts in {}", requestIdCounts.size(), JavaUtils.duration(start));

    for (SingularityRequestIdCount requestIdCount : requestIdCounts) {
      if (!historyPurgingConfiguration.getDeleteTaskHistoryAfterDays().isPresent() && historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest().isPresent() &&
          requestIdCount.getCount() < historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest().get()) {
        LOG.debug("Not purging old taskHistory for {} - {} count is less than {}", requestIdCount.getRequestId(), requestIdCount.getCount(),
            historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest().get());
        continue;
      }

      final long startRequestId = System.currentTimeMillis();

      historyManager.purgeTaskHistory(requestIdCount.getRequestId(), requestIdCount.getCount(), historyPurgingConfiguration.getDeleteTaskHistoryAfterTasksPerRequest(), purgeBefore,
          !historyPurgingConfiguration.isDeleteTaskHistoryBytesInsteadOfEntireRow());

      LOG.info("Purged old taskHistory for {} ({} count) in {}", requestIdCount.getRequestId(), requestIdCount.getCount(), JavaUtils.duration(startRequestId));
    }
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

}
