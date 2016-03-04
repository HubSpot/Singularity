package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.TaskManager;

public abstract class BlendedHistoryHelper<T, Q> {

  protected abstract List<T> getFromZk(Q id);
  protected abstract List<T> getFromHistory(Q id, int historyStart, int numFromHistory);

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
      returned = Lists.newArrayListWithCapacity(fromZk.size() + limitCount);

      returned.addAll(fromZk);
      returned.addAll(getFromHistory(id, limitStart, limitCount));

      Collections.sort(returned, getComparator(id));

      returned = returned.subList(0, Math.min(limitCount, returned.size()));
    }

    return returned;
  }

}
