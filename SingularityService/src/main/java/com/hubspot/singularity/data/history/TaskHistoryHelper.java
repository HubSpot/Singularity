package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class TaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory> {

  private final TaskManager taskManager;
  private final HistoryManager historyManager;

  @Inject
  public TaskHistoryHelper(TaskManager taskManager, HistoryManager historyManager) {
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
  protected List<SingularityTaskIdHistory> getFromZk(String requestId) {
    final List<SingularityTaskId> inactiveTasksInZk = taskManager.getInactiveTaskIdsForRequest(requestId);

    return getHistoriesFor(inactiveTasksInZk);
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(String requestId, int historyStart, int numFromHistory) {
    return historyManager.getTaskHistoryForRequest(requestId, historyStart, numFromHistory);
  }

  public Optional<SingularityTask> getTask(SingularityTaskId taskId) {
    Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);

    if (maybeTask.isPresent()) {
      return maybeTask;
    }

    Optional<SingularityTaskHistory> history = historyManager.getTaskHistory(taskId.getId());

    if (history.isPresent()) {
      return Optional.of(history.get().getTask());
    }

    return Optional.absent();
  }

}
