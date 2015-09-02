package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class DeployTaskHistoryHelper extends BlendedHistoryHelper<SingularityTaskIdHistory, SingularityDeployKey> {

  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  private final SingularityConfiguration singularityConfiguration;

  @Inject
  public DeployTaskHistoryHelper(TaskManager taskManager, HistoryManager historyManager, SingularityConfiguration singularityConfiguration) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
    this.singularityConfiguration = singularityConfiguration;
  }

  private List<SingularityTaskIdHistory> getHistoriesFor(Collection<SingularityTaskId> taskIds) {
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
  protected List<SingularityTaskIdHistory> getFromZk(final SingularityDeployKey deployKey) {
    List<SingularityTaskId> requestTaskIds = taskManager.getTaskIdsForRequest(deployKey.getRequestId());

    final Iterable<SingularityTaskId> deployTaskIds = Iterables.filter(requestTaskIds, new Predicate<SingularityTaskId>() {
      @Override
      public boolean apply(SingularityTaskId input) {
        return input.getDeployId().equals(deployKey.getDeployId());
      }
    });

    return getHistoriesFor(ImmutableList.copyOf(deployTaskIds));
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(final SingularityDeployKey deployKey, int historyStart, int numFromHistory) {
    List<SingularityTaskIdHistory> requestHistories = historyManager.getTaskHistoryForRequest(deployKey.getRequestId(), historyStart, numFromHistory);

    final Iterable<SingularityTaskIdHistory> deployHistories = Iterables.filter(requestHistories, new Predicate<SingularityTaskIdHistory>() {
      @Override
      public boolean apply(SingularityTaskIdHistory input) {
        return input.getTaskId().getDeployId().equals(deployKey.getDeployId());
      }
    });

    return ImmutableList.copyOf(deployHistories);
  }

  public List<SingularityTaskIdHistory> getActiveDeployTasks(SingularityDeployKey key) {
    List<SingularityTaskIdHistory> histories = this.getFromZk(key);

    final Iterable<SingularityTaskIdHistory> activeHistories = Iterables.filter(histories, new Predicate<SingularityTaskIdHistory>() {
      @Override
      public boolean apply(SingularityTaskIdHistory input) {
        return !input.getLastTaskState().get().isDone();
      }
    });

    return ImmutableList.copyOf(activeHistories);
  }

  public List<SingularityTaskIdHistory> getInactiveDeployTasks(SingularityDeployKey key, Integer limitCount, Integer limitStart) {
    // We don't know our limits yet before filtering task state
    Integer limit = (int) (singularityConfiguration.getHistoryPurgingConfiguration().getDeleteTaskHistoryAfterTasksPerRequest().or(10000) * 1.2);
    List<SingularityTaskIdHistory> histories = this.getBlendedHistory(key, 0, limit);

    final Iterable<SingularityTaskIdHistory> inactiveHistories = Iterables.filter(histories, new Predicate<SingularityTaskIdHistory>() {
      @Override
      public boolean apply(SingularityTaskIdHistory input) {
        return input.getLastTaskState().get().isDone();
      }
    });

    ImmutableList<SingularityTaskIdHistory> result = ImmutableList.copyOf(inactiveHistories);

    return result.subList(limitStart, Math.min(result.size(), limitStart + limitCount));
  }

}
