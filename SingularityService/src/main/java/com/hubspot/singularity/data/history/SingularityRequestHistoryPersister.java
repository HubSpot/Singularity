package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityHistoryItem;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister.SingularityRequestHistoryParent;

@Singleton
public class SingularityRequestHistoryPersister extends SingularityHistoryPersister<SingularityRequestHistoryParent> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityRequestHistoryPersister.class);

  private final RequestManager requestManager;
  private final HistoryManager historyManager;

  @Inject
  public SingularityRequestHistoryPersister(SingularityConfiguration configuration, RequestManager requestManager, HistoryManager historyManager) {
    super(configuration);

    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  public static class SingularityRequestHistoryParent implements SingularityHistoryItem {

    private final List<SingularityRequestHistory> history;
    private final String requestId;
    private final long createTime;

    public SingularityRequestHistoryParent(List<SingularityRequestHistory> history, String requestId) {
      this.history = history;
      this.requestId = requestId;

      long newestTimestamp = 0;

      for (SingularityRequestHistory historyItem : history) {
        if (historyItem.getCreatedAt() > newestTimestamp) {
          newestTimestamp = historyItem.getCreatedAt();
        }
      }

      createTime = newestTimestamp;
    }

    @Override
    public long getCreateTimestampForCalculatingHistoryAge() {
      return createTime;
    }

  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Checking request history for persistence");

    final long start = System.currentTimeMillis();

    final List<String> requestIdsWithHistory = requestManager.getRequestIdsWithHistory();

    int numHistoryTransferred = 0;
    int numRequests = 0;

    for (String requestId : requestIdsWithHistory) {
      numRequests++;

      List<SingularityRequestHistory> historyForRequestId = requestManager.getRequestHistory(requestId);
      SingularityRequestHistoryParent requestHistoryParent = new SingularityRequestHistoryParent(historyForRequestId, requestId);

      if (moveToHistoryOrCheckForPurge(requestHistoryParent)) {
        numHistoryTransferred += requestHistoryParent.history.size();
      }
    }

    LOG.info("Transferred {} history updates for {} requests in {}", numHistoryTransferred, numRequests, JavaUtils.duration(start));
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(configuration.getDeleteStaleRequestsFromZkWhenNoDatabaseAfterHours());
  }

  @Override
  protected boolean moveToHistory(SingularityRequestHistoryParent object) {
    for (SingularityRequestHistory requestHistory : object.history) {
      try {
        historyManager.saveRequestHistoryUpdate(requestHistory);
      } catch (Throwable t) {
        LOG.warn("Failed to persist {} into History", requestHistory, t);
        return false;
      }

      requestManager.deleteHistoryItem(requestHistory);
    }

    return true;
  }

  @Override
  protected SingularityDeleteResult purgeFromZk(SingularityRequestHistoryParent object) {
    return requestManager.deleteHistoryParent(object.requestId);
  }

}
