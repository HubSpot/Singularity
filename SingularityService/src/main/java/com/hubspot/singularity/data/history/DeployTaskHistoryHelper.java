package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class DeployTaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory, SingularityDeployKey> {

  private final TaskManager taskManager;
  private final HistoryManager historyManager;

  @Inject
  public DeployTaskHistoryHelper(TaskManager taskManager, HistoryManager historyManager) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
  }

  public List<SingularityTaskIdHistory> getHistoriesFor(Collection<SingularityTaskId> taskIds) {
    Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> map = taskManager.getTaskHistoryUpdates(taskIds);

    List<SingularityTaskIdHistory> histories = Lists.newArrayListWithCapacity(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      List<SingularityTaskHistoryUpdate> historyUpdates = map.get(taskId);

      histories.add(SingularityTaskIdHistory.fromTaskIdAndUpdates(taskId, historyUpdates));
    }

    Collections.sort(histories);

    return histories;
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromZk(SingularityDeployKey deployKey) {
    List<SingularityTaskId> activeTaskIds = taskManager.getTaskIdsForRequest(deployKey.getRequestId());
    Iterator<SingularityTaskId> it = activeTaskIds.iterator();
    while(it.hasNext()) {
      if (!it.next().getDeployId().equals(deployKey.getDeployId())) {
        it.remove();
      }
    }
    return getHistoriesFor(activeTaskIds);
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(SingularityDeployKey deployKey, int historyStart, int numFromHistory) {
    List<SingularityTaskIdHistory> histories = historyManager.getTaskHistoryForRequest(deployKey.getRequestId(), historyStart, numFromHistory);
    Iterator<SingularityTaskIdHistory> it = histories.iterator();
    while(it.hasNext()) {
      if (!it.next().getTaskId().getDeployId().equals(deployKey.getDeployId())) {
        it.remove();
      }
    }
    return histories;
  }

  public List<SingularityTaskIdHistory> getActiveDeployTasks(SingularityDeployKey key, Integer limitCount, Integer limitStart) {
    List<SingularityTaskIdHistory> histories = this.getBlendedHistory(key, limitStart, limitCount);
    Iterator<SingularityTaskIdHistory> it = histories.iterator();
    while(it.hasNext()) {
      if (it.next().getLastTaskState().get().isDone()) {
        it.remove();
      }
    }
    return histories;
  }

  public List<SingularityTaskIdHistory> getInactiveDeployTasks(SingularityDeployKey key, Integer limitCount, Integer limitStart) {
    List<SingularityTaskIdHistory> histories = this.getBlendedHistory(key, limitStart, limitCount);
    Iterator<SingularityTaskIdHistory> it = histories.iterator();
    while(it.hasNext()) {
      if (!it.next().getLastTaskState().get().isDone()) {
        it.remove();
      }
    }
    return histories;
  }

}