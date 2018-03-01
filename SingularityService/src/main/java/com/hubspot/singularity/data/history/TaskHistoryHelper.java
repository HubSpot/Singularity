package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.api.common.OrderDirection;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskHistory;
import com.hubspot.singularity.api.task.SingularityTaskHistoryQuery;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskIdHistory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class TaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory, SingularityTaskHistoryQuery> {

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  private final SingularityConfiguration configuration;

  @Inject
  public TaskHistoryHelper(TaskManager taskManager, HistoryManager historyManager, RequestManager requestManager, SingularityConfiguration configuration) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.requestManager = requestManager;
    this.configuration = configuration;
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
    return getFromZk(getRequestIds(query)).stream()
        .filter(query.getHistoryFilter())
        .sorted(query.getComparator())
        .collect(Collectors.toList());
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(SingularityTaskHistoryQuery query, int historyStart, int numFromHistory) {
    return historyManager.getTaskIdHistory(query.getRequestId(), query.getDeployId(), query.getRunId(), query.getHost(), query.getLastTaskStatus(), query.getStartedBefore(),
        query.getStartedAfter(), query.getUpdatedBefore(), query.getUpdatedAfter(), query.getOrderDirection(), Optional.of(historyStart), numFromHistory);
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

    return Optional.empty();
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

    return Optional.empty();
  }

  @Override
  protected boolean queryUsesZkFirst(SingularityTaskHistoryQuery query) {
    if (configuration.isTaskHistoryQueryUsesZkFirst()) {
      return true;
    }
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

  @Override
  protected Optional<Integer> getTotalCount(SingularityTaskHistoryQuery query) {
    final int numFromZk = (int) getFromZk(getRequestIds(query)).stream().filter(query.getHistoryFilter()).count();
    final int numFromHistory = historyManager.getTaskIdHistoryCount(query.getRequestId(), query.getDeployId(), query.getRunId(), query.getHost(), query.getLastTaskStatus(), query.getStartedBefore(),
        query.getStartedAfter(), query.getUpdatedBefore(), query.getUpdatedAfter());

    return Optional.ofNullable(numFromZk + numFromHistory);
  }

}
