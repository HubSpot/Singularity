package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;

@Singleton
public class SingularityRequestHistoryPersister extends SingularityHistoryPersister {

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

    final List<String> requestIdsWithHistory = requestManager.getRequestIdsWithHistory();
    final Set<String> requestIds = Sets.newHashSet(requestManager.getAllRequestIds());

    int numHistoryTransferred = 0;

    for (String requestId : requestIdsWithHistory) {
      List<SingularityRequestHistory> historyForRequestId = requestManager.getRequestHistory(requestId);

      if (transferToHistoryDB(requestId, historyForRequestId)) {
        numHistoryTransferred += historyForRequestId.size();
      }

      if (!requestIds.contains(requestId)) {
        LOG.debug("Deleting request history parent for {} because it wasn't in active request ids", requestId);

        requestManager.deleteHistoryParent(requestId);
      }
    }

    LOG.info("Transferred {} history updates for {} requests in {}", numHistoryTransferred, requestIdsWithHistory.size(), JavaUtils.duration(start));
  }

  private boolean transferToHistoryDB(String requestId, List<SingularityRequestHistory> historyForRequestId) {
    final long start = System.currentTimeMillis();

    for (SingularityRequestHistory requestHistory : historyForRequestId) {
      try {
        historyManager.saveRequestHistoryUpdate(requestHistory);
      } catch (Throwable t) {
        LOG.warn("Failed to persist {} into History", requestHistory, t);
        return false;
      }

      requestManager.deleteHistoryItem(requestHistory);
    }

    LOG.debug("Moved request history for {} ({} items) from ZK to History in {}", requestId, historyForRequestId.size(), JavaUtils.duration(start));

    return true;
  }

}
