package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.TaskManager;

public class TaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory> {

  private final String requestId;
  private final TaskManager taskManager;
  private final HistoryManager historyManager;

  public TaskHistoryHelper(String requestId, TaskManager taskManager, HistoryManager historyManager) {
    this.requestId = requestId;
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
  protected List<SingularityTaskIdHistory> getFromZk() {
    final List<SingularityTaskId> inactiveTasksInZk = taskManager.getInactiveTaskIdsForRequest(requestId);

    return getHistoriesFor(inactiveTasksInZk);
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(int historyStart, int numFromHistory) {
    return historyManager.getTaskHistoryForRequest(requestId, historyStart, numFromHistory);
  }

}
