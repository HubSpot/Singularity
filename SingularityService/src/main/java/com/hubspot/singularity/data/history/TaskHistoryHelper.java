package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryQuery;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class TaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory, SingularityTaskHistoryQuery> {

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;

  @Inject
  public TaskHistoryHelper(TaskManager taskManager, HistoryManager historyManager, RequestManager requestManager) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.requestManager = requestManager;
  }

  private List<SingularityTaskIdHistory> getFromZk(List<String> requestIds) {
    final List<SingularityTaskId> inactiveTasksInZk = taskManager.getInactiveTaskIds(requestIds);

    return getTaskHistoriesFor(taskManager, inactiveTasksInZk);
  }

  private List<String> getRequestIds(SingularityTaskHistoryQuery query) {
    if (query.getRequestId().isPresent()) {
      return Collections.singletonList(query.getRequestId().get());
    }

    return requestManager.getAllRequestIds();
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromZk(SingularityTaskHistoryQuery query) {
    final List<SingularityTaskIdHistory> filteredHistory = Lists.newArrayList(Iterables.filter(getFromZk(getRequestIds(query)), query.getHistoryFilter()));

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
    for (SingularityTaskIdHistory history : getFromZk(Collections.singletonList(requestId))) {
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

  @Override
  protected boolean queryUsesZkFirst(SingularityTaskHistoryQuery query) {
    if (!query.getRequestId().isPresent()) {
      return false;
    }
    if (query.getLastTaskStatus().isPresent()) {
      return false;
    }
    if (query.getHost().isPresent()) {
      return false;
    }
    if (query.getStartedAfter().isPresent()) {
      return false;
    }
    if (query.getStartedBefore().isPresent()) {
      return false;
    }
    if (query.getOrderDirection().isPresent() && query.getOrderDirection().get() == OrderDirection.ASC) {
      return false;
    }
    return true;
  }

  @Override
  protected Comparator<SingularityTaskIdHistory> getComparator(SingularityTaskHistoryQuery query) {
    return query.getComparator();
  }

}
