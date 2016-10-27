package com.hubspot.singularity.data.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Optional;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.TaskManager;

public abstract class BlendedHistoryHelper<T, Q> {

  private static final Logger LOG = LoggerFactory.getLogger(BlendedHistoryHelper.class);
  protected abstract List<T> getFromZk(Q id);
  protected abstract List<T> getFromHistory(Q id, int historyStart, int numFromHistory);

  protected abstract Optional<Integer> getTotalCount(Q id);

  public List<SingularityTaskIdHistory> getTaskHistoriesFor(TaskManager taskManager, Collection<SingularityTaskId> taskIds) {
    Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(taskIds);
    Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> map = taskManager.getTaskHistoryUpdates(taskIds);

    List<SingularityTaskIdHistory> histories = Lists.newArrayListWithCapacity(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      List<SingularityTaskHistoryUpdate> historyUpdates = map.get(taskId);
      SingularityTask task = tasks.get(taskId);
      if (task != null) {
        histories.add(SingularityTaskIdHistory.fromTaskIdAndTaskAndUpdates(taskId, task, historyUpdates));
      }
    }

    Collections.sort(histories);
    return histories;
  }

  protected boolean queryUsesZkFirst(Q id) {
    return true;
  }

  protected Comparator<T> getComparator(Q id) {
    throw new IllegalStateException("Comparator requested for query which doesn't implement it");
  }

  public Optional<Integer> getBlendedHistoryCount(Q id) {
    return getTotalCount(id);
  }

  public List<T> getBlendedHistory(Q id, Integer limitStart, Integer limitCount) {
    final List<T> fromZk = getFromZk(id);

    List<T> returned = null;

    if (queryUsesZkFirst(id)) {
      final int numFromZk = Math.max(0, Math.min(limitCount, fromZk.size() - limitStart));

      final Integer numFromHistory = limitCount - numFromZk;
      final Integer historyStart = Math.max(0, limitStart - fromZk.size());

      returned = Lists.newArrayListWithCapacity(limitCount);

      if (numFromZk > 0) {
        returned.addAll(fromZk.subList(limitStart, limitStart + numFromZk));
      }

      if (numFromHistory > 0) {
        returned.addAll(getFromHistory(id, historyStart, numFromHistory));
      }
    } else {
      returned = getOrderedFromHistory(id, limitStart, limitCount, fromZk);
    }

    return returned;
  }

  private List<T> getOrderedFromHistory(Q id, Integer limitStart, Integer limitCount, List<T> fromZk) {
    SortedMap<T, Boolean> returnedMap = new TreeMap<>(getComparator(id));
    for (T item : fromZk) {
      returnedMap.put(item, false);
    }

    int historyLimitStart = 0;
    List<T> fromHistory = getFromHistory(id, historyLimitStart, limitCount);
    for (T item : fromHistory) {
      returnedMap.put(item, true);
    }

    int currentStartIndex = 0;
    while (!foundAllFromHistoryAndTrimResults(returnedMap, currentStartIndex, getLastRelevantHistoryItemIndex(returnedMap, currentStartIndex), limitStart, limitCount, fromHistory.size())) {
      if (returnedMap.isEmpty()) {
        return getFromHistory(id, limitStart - currentStartIndex, limitCount);
      } else {
        historyLimitStart += limitCount;
        fromHistory = getFromHistory(id, historyLimitStart, limitCount);
        for (T item : fromHistory) {
          returnedMap.put(item, true);
        }
      }
    }
    return new ArrayList<>(returnedMap.keySet());
  }

  private int getLastRelevantHistoryItemIndex(SortedMap<T, Boolean> returnedMap, Integer currentStartIndex) {
    int highestHistoryItemIndex = 0;
    int index = 0;
    for (Map.Entry<T, Boolean> entry : returnedMap.entrySet()) {
      if (entry.getValue()) {
        highestHistoryItemIndex = index;
      }
      index ++;
    }
    return currentStartIndex + highestHistoryItemIndex;
  }

  private boolean foundAllFromHistoryAndTrimResults(SortedMap<T, Boolean> returnedMap, Integer currentStartIndex, Integer lastRelevantHistoryItemIndex, Integer limitStart, Integer limitCount, int numFromHistory) {
    boolean foundAllFromHistory = false;
    List<T> toRemove = new ArrayList<>();
    if (numFromHistory == 0 || lastRelevantHistoryItemIndex > limitStart + limitCount) {
      List<T> current = new ArrayList<>(returnedMap.keySet());
      toRemove.addAll(current.subList(0, Math.min(limitStart - currentStartIndex, current.size())));
      toRemove.addAll(current.subList(Math.min(limitStart - currentStartIndex + limitCount, current.size()), current.size()));
      foundAllFromHistory = true;
    } else {
      toRemove = toRemove.subList(0, Math.min(Math.min(lastRelevantHistoryItemIndex, limitStart - currentStartIndex), toRemove.size()));
      currentStartIndex += toRemove.size();
    }
    for (T item : toRemove) {
      returnedMap.remove(item);
    }

    if (!foundAllFromHistory) {
      LOG.trace("Current start index is {}, querying for more history", currentStartIndex);
    }

    return foundAllFromHistory;
  }

}
