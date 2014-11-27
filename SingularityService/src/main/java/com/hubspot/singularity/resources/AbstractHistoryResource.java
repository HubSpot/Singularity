package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.transcoders.IdTranscoder;

public abstract class AbstractHistoryResource {

  private final HistoryManager historyManager;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final IdTranscoder<SingularityTaskId> singularityTaskIdTranscoder;

  public AbstractHistoryResource(HistoryManager historyManager, TaskManager taskManager, DeployManager deployManager, final IdTranscoder<SingularityTaskId> singularityTaskIdTranscoder) {
    this.historyManager = historyManager;
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.singularityTaskIdTranscoder = singularityTaskIdTranscoder;
  }

  protected SingularityTaskId getTaskIdObject(String taskId) {
    try {
      return singularityTaskIdTranscoder.fromString(taskId);
    } catch (InvalidSingularityTaskIdException e) {
      throw WebExceptions.badRequest("%s is not a valid task id: %s", taskId, e.getMessage());
    }
  }

  protected SingularityTaskHistory getTaskHistory(SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> history = taskManager.getTaskHistory(taskId);

    if (!history.isPresent()) {
      history = historyManager.getTaskHistory(taskId.getId());
    }

    if (!history.isPresent()) {
      throw WebExceptions.notFound("No history for task %s", taskId);
    }

    return history.get();
  }

  protected SingularityDeployHistory getDeployHistory(String requestId, String deployId) {
    Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(requestId, deployId, true);

    if (deployHistory.isPresent()) {
      return deployHistory.get();
    }

    deployHistory = historyManager.getDeployHistory(requestId, deployId);

    if (!deployHistory.isPresent()) {
      throw WebExceptions.notFound("Deploy history for request %s and deploy %s not found", requestId, deployId);
    }

    return deployHistory.get();
  }

}
