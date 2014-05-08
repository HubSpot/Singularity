package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.collect.Lists;

public abstract class BlendedHistoryHelper<T> {

  protected abstract List<T> getFromZk();
  protected abstract List<T> getFromHistory(int historyStart, int numFromHistory);
    
  public List<T> getBlendedHistory(Integer limitCount, Integer limitStart) {
    final List<T> fromZk = getFromZk();
    
    final int numFromZk = Math.max(0, fromZk.size() - limitStart);
    
    final Integer numFromHistory = limitCount - numFromZk;
    final Integer historyStart = Math.max(0, limitStart - fromZk.size());
    
    List<T> returned = Lists.newArrayListWithCapacity(limitCount);
    
    if (numFromZk > 0) {
      returned.addAll(fromZk.subList(limitStart, limitStart + numFromZk));
    }
    
    if (numFromHistory > 0) {
      returned.addAll(getFromHistory(historyStart, numFromHistory));
    }
      
    return returned;
  }
  
}
