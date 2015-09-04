package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
    return historyManager.getTaskHistoryForDeploy(deployKey.getRequestId(), deployKey.getDeployId(), historyStart, numFromHistory);
  }
}
