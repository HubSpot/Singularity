package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryQuery;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class TaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory, SingularityTaskHistoryQuery> {

  private final TaskManager taskManager;
  private final HistoryManager historyManager;

  @Inject
  public TaskHistoryHelper(TaskManager taskManager, HistoryManager historyManager) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
  }

  private List<SingularityTaskIdHistory> getFromZk(String requestId) {
    final List<SingularityTaskId> inactiveTasksInZk = taskManager.getInactiveTaskIdsForRequest(requestId);

    return getTaskHistoriesFor(taskManager, inactiveTasksInZk);
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromZk(SingularityTaskHistoryQuery query) {
    final List<SingularityTaskIdHistory> filteredHistory = Lists.newArrayList(Iterables.filter(getFromZk(query.getRequestId()), query.getHistoryFilter()));

    Collections.sort(filteredHistory, query.getComparator());

    return filteredHistory;
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(SingularityTaskHistoryQuery query, int historyStart, int numFromHistory) {
    return historyManager.getTaskIdHistory(query.getRequestId(), query.getDeployId(), query.getHost(), query.getLastTaskStatus(), query.getStartedBefore(),
        query.getStartedAfter(), query.getOrderDirection(), Optional.of(historyStart), numFromHistory);
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

  public Optional<SingularityTaskIdHistory> getByRunId(String requestId, String runId) {
    for (SingularityTaskIdHistory history : getFromZk(requestId)) {
      if (history.getRunId().isPresent() && history.getRunId().get().equals(runId)) {
        return Optional.of(history);
      }
    }

    Optional<SingularityTaskHistory> history = historyManager.getTaskHistoryByRunId(requestId, runId);

    if (history.isPresent()) {
      return Optional.of(SingularityTaskIdHistory.fromTaskIdAndTaskAndUpdates(history.get().getTask().getTaskId(), history.get().getTask(), history.get().getTaskUpdates()));
    }

    return Optional.absent();
  }

}
