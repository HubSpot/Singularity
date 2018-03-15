package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.api.deploy.SingularityDeployKey;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskIdHistory;
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
    return historyManager.getTaskIdHistory(Optional.of(deployKey.getRequestId()), Optional.of(deployKey.getDeployId()), Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(historyStart), numFromHistory);
  }

  @Override
  protected Optional<Integer> getTotalCount(SingularityDeployKey deployKey) {
    final int numFromZk = taskManager.getInactiveTaskIdsForDeploy(deployKey.getRequestId(), deployKey.getDeployId()).size();
    final int numFromHistory = historyManager.getTaskIdHistoryCount(Optional.of(deployKey.getRequestId()), Optional.of(deployKey.getDeployId()), Optional.empty(), Optional.empty(), Optional.empty(),
      Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    return Optional.of(numFromZk + numFromHistory);
  }

}
