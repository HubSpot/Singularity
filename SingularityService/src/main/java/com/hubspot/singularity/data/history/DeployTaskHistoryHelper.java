package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployKey;
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

  @Override
  protected List<SingularityTaskIdHistory> getFromZk(final SingularityDeployKey deployKey) {
    List<SingularityTaskId> deployTaskIds = taskManager.getInactiveTaskIdsForDeploy(deployKey.getRequestId(), deployKey.getDeployId());
    return getTaskHistoriesFor(taskManager, deployTaskIds);
  }

  @Override
  protected List<SingularityTaskIdHistory> getFromHistory(final SingularityDeployKey deployKey, int historyStart, int numFromHistory) {
    return historyManager.getTaskIdHistory(Optional.of(deployKey.getRequestId()), Optional.of(deployKey.getDeployId()), Optional.<String>absent(), Optional.<String> absent(), Optional.<ExtendedTaskState> absent(),
        Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<OrderDirection> absent(), Optional.of(historyStart), numFromHistory);
  }

  @Override
  protected Optional<Integer> getTotalCount(SingularityDeployKey deployKey) {
    final int numFromZk = taskManager.getInactiveTaskIdsForDeploy(deployKey.getRequestId(), deployKey.getDeployId()).size();
    final int numFromHistory = historyManager.getTaskIdHistoryCount(Optional.of(deployKey.getRequestId()), Optional.of(deployKey.getDeployId()), Optional.<String>absent(), Optional.<String> absent(), Optional.<ExtendedTaskState> absent(),
      Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent());
    return Optional.of(numFromZk + numFromHistory);
  }

}
