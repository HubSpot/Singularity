package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
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
    Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> map = taskManager.getTaskHistoryUpdates(taskIds);
    
    List<SingularityTaskIdHistory> histories = Lists.newArrayListWithCapacity(taskIds.size());
    
    for (SingularityTaskId taskId : taskIds) {
      Collection<SingularityTaskHistoryUpdate> historyUpdates = map.get(taskId);
      
      histories.add(SingularityTaskIdHistory.fromTaskIdAndUpdates(taskId, historyUpdates));
    }
    
    return Ordering.natural().sortedCopy(histories);
  }
  
  @Override
  protected List<SingularityTaskIdHistory> getFromZk() {
    final List<SingularityTaskId> inactiveTasksInZk = taskManager.getInActiveTaskIdsForRequest(requestId);
    
    return getHistoriesFor(inactiveTasksInZk);
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(int historyStart, int numFromHistory) {
    return historyManager.getTaskHistoryForRequest(requestId, historyStart, numFromHistory);
  }
  
}
