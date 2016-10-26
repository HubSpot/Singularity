package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.data.RequestManager;

@Singleton
public class RequestHistoryHelper extends BlendedHistoryHelper<SingularityRequestHistory, String> {

  private final RequestManager requestManager;
  private final HistoryManager historyManager;

  @Inject
  public RequestHistoryHelper(RequestManager requestManager, HistoryManager historyManager) {
    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  @Override
  protected List<SingularityRequestHistory> getFromZk(String requestId) {
    List<SingularityRequestHistory> requestHistory = requestManager.getRequestHistory(requestId);

    Collections.sort(requestHistory);

    return requestHistory;
  }

  @Override
  protected List<SingularityRequestHistory> getFromHistory(String requestId, int historyStart, int numFromHistory) {
    return historyManager.getRequestHistory(requestId, Optional.of(OrderDirection.DESC), historyStart, numFromHistory);
  }

  public Optional<SingularityRequestHistory> getFirstHistory(String requestId) {
    Optional<SingularityRequestHistory> firstHistory = JavaUtils.getFirst(historyManager.getRequestHistory(requestId, Optional.of(OrderDirection.ASC), 0, 1));

    if (firstHistory.isPresent()) {
      return firstHistory;
    }

    return JavaUtils.getLast(getFromZk(requestId));
  }

  public Optional<SingularityRequestHistory> getLastHistory(String requestId) {
    Optional<SingularityRequestHistory> lastHistory = JavaUtils.getFirst(getFromZk(requestId));

    if (lastHistory.isPresent()) {
      return lastHistory;
    }

    return JavaUtils.getFirst(historyManager.getRequestHistory(requestId, Optional.of(OrderDirection.DESC), 0, 1));
  }

  @Override
  protected Optional<Integer> getTotalCount(String requestId) {
    int numFromZk = requestManager.getRequestHistory(requestId).size();
    int numFromHistory = historyManager.getRequestHistoryCount(requestId);

    return Optional.of(numFromZk + numFromHistory);
  }

}
