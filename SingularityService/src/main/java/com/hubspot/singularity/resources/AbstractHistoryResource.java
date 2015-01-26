package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;

public abstract class AbstractHistoryResource {

  private final HistoryManager historyManager;
  private final TaskManager taskManager;
  private final DeployManager deployManager;

  public AbstractHistoryResource(HistoryManager historyManager, TaskManager taskManager, DeployManager deployManager) {
    this.historyManager = historyManager;
    this.taskManager = taskManager;
    this.deployManager = deployManager;
  }

  protected SingularityTaskId getTaskIdObject(String taskId) {
    try {
      return SingularityTaskId.valueOf(taskId);
    } catch (InvalidSingularityTaskIdException e) {
      throw badRequest("%s is not a valid task id: %s", taskId, e.getMessage());
    }
  }

  protected SingularityTaskHistory getTaskHistory(SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> history = taskManager.getTaskHistory(taskId);

    if (!history.isPresent()) {
      history = historyManager.getTaskHistory(taskId.getId());
    }

    checkNotFound(history.isPresent(), "No history for task %s", taskId);

    return history.get();
  }

  protected SingularityDeployHistory getDeployHistory(String requestId, String deployId) {
    Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(requestId, deployId, true);

    if (deployHistory.isPresent()) {
      return deployHistory.get();
    }

    deployHistory = historyManager.getDeployHistory(requestId, deployId);

    checkNotFound(deployHistory.isPresent(), "Deploy history for request %s and deploy %s not found", requestId, deployId);

    return deployHistory.get();
  }

}
