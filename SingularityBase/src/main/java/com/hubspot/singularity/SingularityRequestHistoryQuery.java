package com.hubspot.singularity;

import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import java.util.Comparator;
import java.util.Optional;

public class SingularityRequestHistoryQuery {

  private final String requestId;
  private final Optional<Long> createdBefore;
  private final Optional<Long> createdAfter;
  private final Optional<OrderDirection> orderDirection;

  public SingularityRequestHistoryQuery(
    String requestId,
    Optional<Long> createdBefore,
    Optional<Long> createdAfter,
    Optional<OrderDirection> orderDirection
  ) {
    this.requestId = requestId;
    this.createdBefore = createdBefore;
    this.createdAfter = createdAfter;
    this.orderDirection = orderDirection;
  }

  public String getRequestId() {
    return requestId;
  }

  public Optional<Long> getCreatedBefore() {
    return createdBefore;
  }

  public Optional<Long> getCreatedAfter() {
    return createdAfter;
  }

  public Optional<OrderDirection> getOrderDirection() {
    return orderDirection;
  }

  public Predicate<SingularityRequestHistory> getHistoryFilter() {
    return new Predicate<SingularityRequestHistory>() {
      @Override
      public boolean apply(SingularityRequestHistory input) {
        if (!requestId.equals(input.getRequest().getId())) {
          return false;
        }

        if (createdAfter.isPresent() && createdAfter.get() >= input.getCreatedAt()) {
          return false;
        }

        if (createdBefore.isPresent() && createdBefore.get() <= input.getCreatedAt()) {
          return false;
        }

        return true;
      }
    };
  }

  public Comparator<SingularityRequestHistory> getComparator() {
    final OrderDirection localOrderDirection = orderDirection.orElse(OrderDirection.DESC);

    return new Comparator<SingularityRequestHistory>() {
      @Override
      public int compare(SingularityRequestHistory o1, SingularityRequestHistory o2) {
        ComparisonChain chain = ComparisonChain.start();

        if (localOrderDirection == OrderDirection.ASC) {
          chain = chain.compare(o1.getCreatedAt(), o2.getCreatedAt());
        } else {
          chain = chain.compare(o2.getCreatedAt(), o1.getCreatedAt());
        }

        return chain.compare(o1.getRequest().getId(), o2.getRequest().getId()).result();
      }
    };
  }

  @Override
  public String toString() {
    return (
      "SingularityRequestHistoryQuery{" +
      "requestId=" +
      requestId +
      ", createdBefore=" +
      createdBefore +
      ", createdAfter=" +
      createdAfter +
      ", orderDirection=" +
      orderDirection +
      '}'
    );
  }
}
