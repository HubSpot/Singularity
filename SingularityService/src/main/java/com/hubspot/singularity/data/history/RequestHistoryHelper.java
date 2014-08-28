package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;

public class RequestHistoryHelper extends BlendedHistoryHelper<SingularityRequestHistory> {

  private final String requestId;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;

  public RequestHistoryHelper(String requestId, RequestManager requestManager, HistoryManager historyManager) {
    this.requestId = requestId;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  @Override
  protected List<SingularityRequestHistory> getFromZk() {
    List<SingularityRequestHistory> requestHistory = requestManager.getRequestHistory(requestId);
    
    Collections.sort(requestHistory);

    return requestHistory;
  }

  @Override
  protected List<SingularityRequestHistory> getFromHistory(int historyStart, int numFromHistory) {
    return historyManager.getRequestHistory(requestId, Optional.of(OrderDirection.DESC), historyStart, numFromHistory);
  }

}
