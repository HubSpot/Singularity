package com.hubspot.singularity.data.history;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistoryQuery;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
public class RequestHistoryHelper
  extends BlendedHistoryHelper<SingularityRequestHistory, SingularityRequestHistoryQuery> {

  private final RequestManager requestManager;
  private final HistoryManager historyManager;

  @Inject
  public RequestHistoryHelper(
    RequestManager requestManager,
    HistoryManager historyManager,
    SingularityConfiguration configuration
  ) {
    super(configuration.getDatabaseConfiguration().isPresent());
    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  @Override
  protected List<SingularityRequestHistory> getFromZk(
    SingularityRequestHistoryQuery query
  ) {
    List<SingularityRequestHistory> requestHistory = requestManager.getRequestHistory(
      query.getRequestId()
    );
    final List<SingularityRequestHistory> filteredHistory = Lists.newArrayList(
      Iterables.filter(requestHistory, query.getHistoryFilter())
    );

    Collections.sort(filteredHistory, query.getComparator());

    return filteredHistory;
  }

  @Override
  protected List<SingularityRequestHistory> getFromHistory(
    SingularityRequestHistoryQuery query,
    int historyStart,
    int numFromHistory
  ) {
    return historyManager.getRequestHistory(
      query.getRequestId(),
      query.getCreatedBefore(),
      query.getCreatedAfter(),
      query.getOrderDirection(),
      historyStart,
      numFromHistory
    );
  }

  public Optional<SingularityRequestHistory> getFirstHistory(String requestId) {
    Optional<SingularityRequestHistory> firstHistory = JavaUtils.getFirst(
      historyManager.getRequestHistory(
        requestId,
        Optional.empty(),
        Optional.empty(),
        Optional.of(OrderDirection.ASC),
        0,
        1
      )
    );

    if (firstHistory.isPresent()) {
      return firstHistory;
    }

    return JavaUtils.getLast(
      getFromZk(
        new SingularityRequestHistoryQuery(
          requestId,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      )
    );
  }

  public Optional<SingularityRequestHistory> getLastHistory(String requestId) {
    Optional<SingularityRequestHistory> lastHistory = JavaUtils.getFirst(
      getFromZk(
        new SingularityRequestHistoryQuery(
          requestId,
          Optional.empty(),
          Optional.empty(),
          Optional.empty()
        )
      )
    );

    if (lastHistory.isPresent()) {
      return lastHistory;
    }

    return JavaUtils.getFirst(
      historyManager.getRequestHistory(
        requestId,
        Optional.empty(),
        Optional.empty(),
        Optional.of(OrderDirection.DESC),
        0,
        1
      )
    );
  }

  @Override
  protected Optional<Integer> getTotalCount(
    SingularityRequestHistoryQuery query,
    boolean canSkipZk
  ) {
    int numFromZk;
    if (sqlEnabled && canSkipZk) {
      numFromZk = 0;
    } else {
      numFromZk = requestManager.getRequestHistory(query.getRequestId()).size();
    }
    int numFromHistory = historyManager.getRequestHistoryCount(query.getRequestId());

    return Optional.of(numFromZk + numFromHistory);
  }
}
