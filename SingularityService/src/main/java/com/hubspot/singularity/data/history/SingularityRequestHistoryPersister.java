package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;

@Singleton
public class SingularityRequestHistoryPersister extends SingularityHistoryPersister<SingularityRequestHistory> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityRequestHistoryPersister.class);

  private final RequestManager requestManager;
  private final HistoryManager historyManager;

  @Inject
  public SingularityRequestHistoryPersister(SingularityConfiguration configuration, RequestManager requestManager, HistoryManager historyManager) {
    super(configuration);

    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Checking request history for persistence");

    final long start = System.currentTimeMillis();

    int numHistoryTransferred = 0;

    final List<String> requestIds = requestManager.getRequestIdsWithHistory();

    for (String requestId : requestIds) {
      final List<SingularityRequestHistory> requestHistoryItems = requestManager.getRequestHistory(requestId);
      Collections.sort(requestHistoryItems);  // default ordering is createdAt descending

      int i = 0;
      for (SingularityRequestHistory requestHistory : requestHistoryItems) {
        if (moveToHistoryOrCheckForPurge(requestHistory, i++)) {
          numHistoryTransferred++;
        }
      }
    }

    LOG.info("Transferred {} history updates for {} requests in {}", numHistoryTransferred, requestIds.size(), JavaUtils.duration(start));
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(configuration.getDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours());
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxRequestHistoryUpdatesPerRequestInZkWhenNoDatabase();
  }

  @Override
  protected boolean moveToHistory(SingularityRequestHistory object) {
    try {
      historyManager.saveRequestHistoryUpdate(object);
    } catch (Throwable t) {
      LOG.warn("Failed to persist {} into History", object, t);
      return false;
    }

    requestManager.deleteHistoryItem(object);

    return true;
  }

  @Override
  protected SingularityDeleteResult purgeFromZk(SingularityRequestHistory object) {
    return requestManager.deleteHistoryItem(object);
  }

}
