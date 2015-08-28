package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.collect.Lists;

public abstract class BlendedHistoryHelper<T, Q> {

  protected abstract List<T> getFromZk(Q id);
  protected abstract List<T> getFromHistory(Q id, int historyStart, int numFromHistory);

  public List<T> getBlendedHistory(Q id, Integer limitStart, Integer limitCount) {
    final List<T> fromZk = getFromZk(id);

    final int numFromZk = Math.max(0, Math.min(limitCount, fromZk.size() - limitStart));

    final Integer numFromHistory = limitCount - numFromZk;
    final Integer historyStart = Math.max(0, limitStart - fromZk.size());

    List<T> returned = Lists.newArrayListWithCapacity(limitCount);

    if (numFromZk > 0) {
      returned.addAll(fromZk.subList(limitStart, limitStart + numFromZk));
    }

    if (numFromHistory > 0) {
      returned.addAll(getFromHistory(id, historyStart, numFromHistory));
    }

    return returned;
  }

}
